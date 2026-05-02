import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { FooterComponent } from './footer/footer.component';

@Component({
  selector: 'app-frontoffice-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, HeaderComponent, FooterComponent],
  templateUrl: './frontoffice-layout.component.html',
  styleUrls: ['./frontoffice-layout.component.scss']
})
export class FrontofficeLayoutComponent {}
