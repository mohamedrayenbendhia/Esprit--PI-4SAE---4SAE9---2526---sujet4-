import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * Surligne les occurrences d'un terme recherché dans un texte.
 *
 * Usage : {{ text | searchHighlight: searchTerm }}
 *
 * Retourne du SafeHtml — utiliser avec [innerHTML] dans le template.
 *
 * Exemple :
 *   <span [innerHTML]="c.subject | searchHighlight:searchQuery()"></span>
 */
@Pipe({
  name: 'searchHighlight',
  standalone: true,
  pure: true   // recalcul uniquement quand les inputs changent
})
export class SearchHighlightPipe implements PipeTransform {

  constructor(private sanitizer: DomSanitizer) {}

  transform(text: string, search: string): SafeHtml {
    if (!search || !text) {
      return this.sanitizer.bypassSecurityTrustHtml(this.escape(text ?? ''));
    }

    const term    = search.trim();
    if (!term) {
      return this.sanitizer.bypassSecurityTrustHtml(this.escape(text));
    }

    // Échapper les caractères spéciaux du terme pour l'utiliser en RegExp
    const escaped  = term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const regex    = new RegExp(`(${escaped})`, 'gi');
    const highlighted = this.escape(text).replace(
      // On travaille sur le texte déjà échappé → on surligne le terme échappé
      new RegExp(`(${this.escape(term).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi'),
      '<mark>$1</mark>'
    );

    return this.sanitizer.bypassSecurityTrustHtml(highlighted);
  }

  /** Échappe les caractères HTML pour éviter les injections */
  private escape(text: string): string {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }
}