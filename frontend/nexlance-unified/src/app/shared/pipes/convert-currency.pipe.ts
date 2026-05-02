import { Pipe, PipeTransform, inject, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { CurrencyExchangeService } from '../../core/services/currency-exchange.service';
import { Currency } from '../models/contract.model';
import { Subscription } from 'rxjs';

/**
 * Pipe pour convertir un montant en temps réel.
 * Usage: {{ 5000 | convertCurrency:'EUR':'USD' | async }}
 * Retourne une string formatée: "5 400.00 $"
 */
@Pipe({
  name: 'convertCurrency',
  standalone: true,
  pure: false
})
export class ConvertCurrencyPipe implements PipeTransform, OnDestroy {

  private currencyService = inject(CurrencyExchangeService);
  private lastFrom?: Currency;
  private lastTo?: Currency;
  private lastAmount?: number;
  private cachedResult: string = '';
  private subscription?: Subscription;

  transform(amount: number | null | undefined, from: Currency, to: Currency): string {
    if (amount == null) return '';
    if (from === to) return `${amount.toLocaleString('fr-FR', { minimumFractionDigits: 2 })} ${this.currencyService.getCurrencySymbol(to)}`;

    // Return cached if inputs haven't changed
    if (amount === this.lastAmount && from === this.lastFrom && to === this.lastTo && this.cachedResult) {
      return this.cachedResult;
    }

    this.lastAmount = amount;
    this.lastFrom = from;
    this.lastTo = to;

    this.subscription?.unsubscribe();
    this.subscription = this.currencyService.convert(amount, from, to).subscribe(result => {
      const symbol = this.currencyService.getCurrencySymbol(to);
      this.cachedResult = `${result.convertedAmount.toLocaleString('fr-FR', { minimumFractionDigits: 2 })} ${symbol}`;
    });

    return this.cachedResult || `${amount.toLocaleString('fr-FR', { minimumFractionDigits: 2 })} ${this.currencyService.getCurrencySymbol(from)}`;
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }
}
