import {
  Component, Input, OnChanges, OnDestroy, SimpleChanges,
  signal, inject, DestroyRef, ElementRef, ViewChild, AfterViewInit, NgZone
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import * as L from 'leaflet';
import { OrganizationService } from '../../../core/services/organization.service';
import { GeoLocationResponse } from '../../../core/models/organization.model';

// ── Fix Leaflet default marker icons (broken with Webpack bundlers) ────────────
const iconDefault = L.icon({
  iconUrl:       'assets/leaflet/marker-icon.png',
  iconRetinaUrl: 'assets/leaflet/marker-icon-2x.png',
  shadowUrl:     'assets/leaflet/marker-shadow.png',
  iconSize:    [25, 41],
  iconAnchor:  [12, 41],
  popupAnchor: [1, -34],
  shadowSize:  [41, 41]
});
L.Marker.prototype.options.icon = iconDefault;

/**
 * Affiche la localisation d'une organisation sur une carte Leaflet (OpenStreetMap).
 *
 * - Si lat/lon sont disponibles → rendu de la carte + marqueur + popup
 * - Si non géocodé → message d'état + bouton de géocodage (owner/manager uniquement)
 *
 * Usage :
 *   <app-org-map [orgId]="orgId" [canGeocode]="isOwner" />
 */
@Component({
  selector: 'app-org-map',
  standalone: true,
  imports: [
    CommonModule, MatIconModule, MatButtonModule,
    MatProgressSpinnerModule, MatTooltipModule
  ],
  templateUrl: './org-map.component.html',
  styleUrls:   ['./org-map.component.scss']
})
export class OrgMapComponent implements OnChanges, AfterViewInit, OnDestroy {

  @Input({ required: true }) orgId!: string;
  /** true si l'utilisateur courant est owner ou manager — affiche le bouton "Géocoder" */
  @Input() canGeocode = false;
  /** Nom de l'organisation (pour la popup Leaflet) */
  @Input() orgName  = '';

  @ViewChild('mapContainer', { static: false }) mapContainer!: ElementRef<HTMLDivElement>;

  private orgService = inject(OrganizationService);
  private destroyRef = inject(DestroyRef);
  private zone       = inject(NgZone);

  geo        = signal<GeoLocationResponse | null>(null);
  isLoading  = signal(true);
  isGeocoding = signal(false);
  error      = signal<string | null>(null);

  private map:    L.Map | null    = null;
  private marker: L.Marker | null = null;
  private viewReady = false;

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  ngOnChanges(changes: SimpleChanges) {
    if (changes['orgId'] && this.orgId) {
      this.loadLocation();
    }
  }

  ngAfterViewInit() {
    this.viewReady = true;
    // Si les données sont déjà chargées avant que la vue soit prête
    const g = this.geo();
    if (g?.geocoded) {
      this.initMap(g.latitude!, g.longitude!);
    }
  }

  ngOnDestroy() {
    this.destroyMap();
  }

  // ── Data loading ──────────────────────────────────────────────────────────

  loadLocation() {
    this.isLoading.set(true);
    this.error.set(null);
    this.destroyMap();

    this.orgService.getLocation(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: geo => {
          this.geo.set(geo);
          this.isLoading.set(false);
          if (geo.geocoded && this.viewReady) {
            // Délai pour laisser Angular mettre à jour le DOM
            setTimeout(() => this.initMap(geo.latitude!, geo.longitude!), 50);
          }
        },
        error: () => {
          this.error.set('Failed to load location.');
          this.isLoading.set(false);
        }
      });
  }

  /** Déclenche le géocodage à la demande via Nominatim (backend) */
  triggerGeocode() {
    this.isGeocoding.set(true);
    this.error.set(null);

    this.orgService.geocodeOrganization(this.orgId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: geo => {
          this.geo.set(geo);
          this.isGeocoding.set(false);
          if (geo.geocoded) {
            setTimeout(() => this.initMap(geo.latitude!, geo.longitude!), 50);
          } else {
            this.error.set('Address not found on the map. Please check the "Location" field.');
          }
        },
        error: () => {
          this.error.set('Geocoding failed. Please try again later.');
          this.isGeocoding.set(false);
        }
      });
  }

  // ── Leaflet map ────────────────────────────────────────────────────────────

  private initMap(lat: number, lon: number) {
    this.zone.runOutsideAngular(() => {
      this.destroyMap();

      if (!this.mapContainer?.nativeElement) return;

      this.map = L.map(this.mapContainer.nativeElement, {
        center:         [lat, lon],
        zoom:           13,
        zoomControl:    true,
        scrollWheelZoom: false   // désactiver le zoom souris sur un profil (UX)
      });

      // Tuiles OpenStreetMap — cohérent avec Nominatim
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19
      }).addTo(this.map);

      // Marqueur avec popup
      const popupContent = `
        <div class="map-popup">
          <strong>${this.orgName || 'Organization'}</strong><br/>
          <small>${this.geo()?.address ?? ''}</small>
        </div>
      `;

      this.marker = L.marker([lat, lon])
        .addTo(this.map)
        .bindPopup(popupContent, { maxWidth: 200 })
        .openPopup();
    });
  }

  private destroyMap() {
    if (this.map) {
      this.map.remove();
      this.map    = null;
      this.marker = null;
    }
  }
}
