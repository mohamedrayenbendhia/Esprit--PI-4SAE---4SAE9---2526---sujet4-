import {
  Component, Input, Output, EventEmitter,
  signal, computed, ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

// ── Constantes ────────────────────────────────────────────────
export const ALLOWED_MIME_TYPES = [
  'image/jpeg', 'image/png', 'image/gif', 'image/webp',
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/msword'
];
export const ALLOWED_EXTENSIONS = [
  '.jpg', '.jpeg', '.png', '.gif', '.webp',
  '.pdf', '.xlsx', '.xls', '.docx', '.doc'
];
export const MAX_FILE_SIZE_MB    = 10;
export const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;
export const MAX_FILES           = 5;

// ── Modèle ────────────────────────────────────────────────────
export interface ManagedFile {
  id:               string;         // identifiant unique local
  file?:            File;           // présent si upload local (création)
  name:             string;         // nom affiché (renommable)
  originalName:     string;         // nom d'origine (non modifiable)
  sizeLabel:        string;
  mimeType:         string;
  typeIcon:         string;
  typeLabel:        string;
  previewUrl?:      string;         // data-URL pour images (local) ou URL serveur
  safePreviewUrl?:  SafeResourceUrl; // URL sanitisée pour PDF iframe
  isImage:          boolean;
  isPdf:            boolean;
  isRenaming:       boolean;        // état UI rename inline
  tempName:         string;         // valeur en cours dans l'input rename
  uploadedUrl?:     string;         // URL serveur une fois uploadé
}

@Component({
  selector: 'app-attachment-manager',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './attachment-manager.component.html',
  styleUrls: ['./attachment-manager.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AttachmentManagerComponent {

  constructor(private sanitizer: DomSanitizer) {}

  // ── Inputs / Outputs ──────────────────────────────────────

  /** Mode lecture seule : utilisé dans complaint-detail pour tous les rôles */
  @Input() readonly = false;

  /** Fichiers existants venant du serveur (URLs en string[]) */
  @Input() set existingAttachments(urls: string[]) {
    if (!urls?.length) return;
    const serverFiles: ManagedFile[] = urls.map(url => this.fromUrl(url));
    this.files.set(serverFiles);
  }

  /** Émet la liste des ManagedFile à chaque changement */
  @Output() filesChanged = new EventEmitter<ManagedFile[]>();

  // ── State ─────────────────────────────────────────────────
  files      = signal<ManagedFile[]>([]);
  fileError  = signal<string | null>(null);
  preview    = signal<ManagedFile | null>(null);   // modal preview

  // ── Computed ──────────────────────────────────────────────
  canAddMore    = computed(() => !this.readonly && this.files().length < MAX_FILES);
  isPreviewable = (f: ManagedFile) => f.isImage || f.isPdf;
  totalFiles = computed(() => this.files().length);

  allowedExtensions = ALLOWED_EXTENSIONS.join(', ');
  maxFileSizeMb     = MAX_FILE_SIZE_MB;
  maxFiles          = MAX_FILES;

  // ── Sélection fichiers ────────────────────────────────────

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.fileError.set(null);

    const current = this.files();
    if (current.length + input.files.length > MAX_FILES) {
      this.fileError.set(`Maximum ${MAX_FILES} files allowed.`);
      input.value = '';
      return;
    }

    Array.from(input.files).forEach(file => {
      const ext    = '.' + file.name.split('.').pop()?.toLowerCase();
      const typeOk = ALLOWED_MIME_TYPES.includes(file.type) || ALLOWED_EXTENSIONS.includes(ext);

      if (!typeOk) {
        this.fileError.set(`File type not allowed: ${file.name}.`);
        return;
      }
      if (file.size > MAX_FILE_SIZE_BYTES) {
        this.fileError.set(`File too large: ${file.name} (max ${MAX_FILE_SIZE_MB} MB).`);
        return;
      }
      if (current.some(f => f.originalName === file.name && f.file?.size === file.size)) {
        this.fileError.set(`Already added: ${file.name}`);
        return;
      }

      const managed = this.fromFile(file);
      this.files.update(f => [...f, managed]);

      // Génère le previewUrl pour les images
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = e => {
          this.files.update(list =>
            list.map(f => f.id === managed.id
              ? { ...f, previewUrl: e.target?.result as string }
              : f
            )
          );
        };
        reader.readAsDataURL(file);
      }
    });

    input.value = '';
    this.emit();
  }

  // ── Suppression ───────────────────────────────────────────

  removeFile(id: string): void {
    const f = this.files().find(f => f.id === id);
    if (f?.previewUrl && f.file) {
      // Libère la mémoire si c'était un object URL
      URL.revokeObjectURL(f.previewUrl);
    }
    this.files.update(list => list.filter(f => f.id !== id));
    this.fileError.set(null);
    this.emit();
  }

  // ── Renommage inline ──────────────────────────────────────

  startRename(id: string): void {
    this.files.update(list =>
      list.map(f => f.id === id
        ? { ...f, isRenaming: true, tempName: f.name }
        : { ...f, isRenaming: false }
      )
    );
  }

  confirmRename(id: string): void {
    this.files.update(list =>
      list.map(f => {
        if (f.id !== id) return f;
        const raw      = f.tempName.trim();
        const ext      = '.' + f.originalName.split('.').pop()!;
        // Garde l'extension d'origine si l'utilisateur l'a supprimée
        const newName  = raw
          ? (raw.endsWith(ext) ? raw : raw + ext)
          : f.originalName;
        return { ...f, name: newName, isRenaming: false };
      })
    );
    this.emit();
  }

  cancelRename(id: string): void {
    this.files.update(list =>
      list.map(f => f.id === id ? { ...f, isRenaming: false } : f)
    );
  }

  onRenameKeydown(event: KeyboardEvent, id: string): void {
    if (event.key === 'Enter')  { event.preventDefault(); this.confirmRename(id); }
    if (event.key === 'Escape') { event.preventDefault(); this.cancelRename(id); }
  }

  // ── Preview modal ─────────────────────────────────────────

  openPreview(file: ManagedFile): void {
    // Pour les PDF locaux sans safePreviewUrl encore générée, en créer une
    if (file.isPdf && file.file && !file.safePreviewUrl) {
      const objectUrl = URL.createObjectURL(file.file);
      this.files.update(list => list.map(f =>
        f.id === file.id
          ? { ...f, safePreviewUrl: this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl) }
          : f
      ));
      // Re-lire la version mise à jour
      const updated = this.files().find(f => f.id === file.id) ?? file;
      this.preview.set(updated);
    } else {
      this.preview.set(file);
    }
  }

  closePreview(): void {
    this.preview.set(null);
  }

  // ── Téléchargement ────────────────────────────────────────

  downloadFile(file: ManagedFile): void {
    if (file.file) {
      const url  = URL.createObjectURL(file.file);
      const link = document.createElement('a');
      link.href  = url;
      link.download = file.name;
      link.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } else if (file.uploadedUrl) {
      window.open(file.uploadedUrl, '_blank');
    } else {
      this.fileError.set(`Cannot download: URL not available for ${file.name}`);
    }
  }

  // ── Helpers publics ───────────────────────────────────────

  /** Retourne la liste des ManagedFile (utilisé par le parent) */
  getFiles(): ManagedFile[] {
    return this.files();
  }

  private emit(): void {
    this.filesChanged.emit(this.files());
  }

  // ── Factories ─────────────────────────────────────────────

  private fromFile(file: File): ManagedFile {
    const ext   = '.' + file.name.split('.').pop()?.toLowerCase();
    const isPdf = file.type === 'application/pdf' || ext === '.pdf';
    const managed: ManagedFile = {
      id:           crypto.randomUUID(),
      file,
      name:         file.name,
      originalName: file.name,
      sizeLabel:    this.formatSize(file.size),
      mimeType:     file.type,
      typeIcon:     this.getTypeIcon(file.type, ext),
      typeLabel:    this.getTypeLabel(file.type, ext),
      isImage:      file.type.startsWith('image/'),
      isPdf,
      isRenaming:   false,
      tempName:     file.name
    };
    if (isPdf) {
      const objectUrl = URL.createObjectURL(file);
      managed.safePreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl);
    }
    return managed;
  }

  private fromUrl(url: string): ManagedFile {
    const name  = url.split('/').pop() ?? url;
    const ext   = '.' + name.split('.').pop()?.toLowerCase();
    const isImg = ['.jpg', '.jpeg', '.png', '.gif', '.webp'].includes(ext);
    const isPdf = ext === '.pdf';
    return {
      id:             crypto.randomUUID(),
      name,
      originalName:   name,
      sizeLabel:      '',
      mimeType:       '',
      typeIcon:       this.getTypeIcon('', ext),
      typeLabel:      this.getTypeLabel('', ext),
      isImage:        isImg,
      isPdf,
      isRenaming:     false,
      tempName:       name,
      uploadedUrl:    url,
      previewUrl:     isImg ? url : undefined,
      safePreviewUrl: isPdf ? this.sanitizer.bypassSecurityTrustResourceUrl(url) : undefined
    };
  }

  // ── Utilitaires privés ────────────────────────────────────

  private getTypeIcon(mime: string, ext: string): string {
    if (mime.startsWith('image/'))    return 'image';
    if (mime === 'application/pdf' || ext === '.pdf')   return 'picture_as_pdf';
    if (['.xlsx', '.xls'].includes(ext)) return 'table_chart';
    if (['.docx', '.doc'].includes(ext)) return 'description';
    return 'attach_file';
  }

  private getTypeLabel(mime: string, ext: string): string {
    if (mime.startsWith('image/'))    return 'Image';
    if (mime === 'application/pdf' || ext === '.pdf')   return 'PDF';
    if (['.xlsx', '.xls'].includes(ext)) return 'Excel';
    if (['.docx', '.doc'].includes(ext)) return 'Word';
    return 'File';
  }

  private formatSize(bytes: number): string {
    if (!bytes)            return '';
    if (bytes < 1024)      return `${bytes} B`;
    if (bytes < 1024*1024) return `${(bytes/1024).toFixed(1)} KB`;
    return `${(bytes/(1024*1024)).toFixed(1)} MB`;
  }
}