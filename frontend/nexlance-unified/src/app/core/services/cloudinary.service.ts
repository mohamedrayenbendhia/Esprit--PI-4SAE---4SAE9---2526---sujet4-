import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CloudinaryUploadResult {
  public_id: string;
  secure_url: string;
  url: string;
  original_filename: string;
  format: string;
  bytes: number;
  width?: number;
  height?: number;
  created_at: string;
  version: number;
  version_id: string;
  resource_type: string;
}

export interface CloudinaryFile {
  publicId: string;
  url: string;
  fileName: string;
  format: string;
  size: number;
  version: number;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class CloudinaryService {
  private cloudName: string;
  private uploadPreset: string;
  private apiUrl: string;

  constructor(private http: HttpClient) {
    this.cloudName = environment.cloudinary.cloudName;
    this.uploadPreset = environment.cloudinary.uploadPreset;
    this.apiUrl = `${environment.cloudinary.apiUrl}/${this.cloudName}`;
  }

  /**
   * Upload a single file to Cloudinary (unsigned upload)
   * @param file The file to upload
   * @param folder Optional folder path in Cloudinary
   */
  uploadFile(file: File, folder: string = 'nexlance'): Observable<CloudinaryFile> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_preset', this.uploadPreset);
    formData.append('folder', folder);

    const resourceType = this.getResourceType(file);

    return this.http.post<CloudinaryUploadResult>(
      `${this.apiUrl}/${resourceType}/upload`,
      formData
    ).pipe(
      map(result => ({
        publicId: result.public_id,
        url: result.secure_url,
        fileName: result.original_filename,
        format: result.format,
        size: result.bytes,
        version: result.version,
        createdAt: result.created_at
      }))
    );
  }

  /**
   * Upload multiple files to Cloudinary
   */
  uploadFiles(files: File[], folder: string = 'nexlance'): Observable<CloudinaryFile[]> {
    const uploads = files.map(file => this.uploadFile(file, folder));
    return new Observable(subscriber => {
      const results: CloudinaryFile[] = [];
      let completed = 0;

      uploads.forEach(upload$ => {
        upload$.subscribe({
          next: result => {
            results.push(result);
            completed++;
            if (completed === files.length) {
              subscriber.next(results);
              subscriber.complete();
            }
          },
          error: err => {
            console.error('Cloudinary upload error:', err);
            completed++;
            if (completed === files.length) {
              subscriber.next(results);
              subscriber.complete();
            }
          }
        });
      });
    });
  }

  /**
   * Upload a job offer attachment
   */
  uploadJobAttachment(file: File): Observable<CloudinaryFile> {
    return this.uploadFile(file, 'nexlance/job-attachments');
  }

  /**
   * Upload a deliverable file (milestone submission)
   */
  uploadDeliverable(file: File, projectId: string, milestoneId: string): Observable<CloudinaryFile> {
    return this.uploadFile(file, `nexlance/deliverables/${projectId}/${milestoneId}`);
  }

  /**
   * Upload user avatar
   */
  uploadAvatar(file: File, userId: string): Observable<CloudinaryFile> {
    return this.uploadFile(file, `nexlance/avatars/${userId}`);
  }

  /**
   * Get optimized image URL with transformations
   */
  getOptimizedUrl(publicId: string, options: {
    width?: number;
    height?: number;
    crop?: string;
    quality?: string;
    format?: string;
  } = {}): string {
    const transforms: string[] = [];

    if (options.width) transforms.push(`w_${options.width}`);
    if (options.height) transforms.push(`h_${options.height}`);
    if (options.crop) transforms.push(`c_${options.crop}`);
    if (options.quality) transforms.push(`q_${options.quality}`);
    if (options.format) transforms.push(`f_${options.format}`);

    const transformStr = transforms.length > 0 ? transforms.join(',') + '/' : '';
    return `https://res.cloudinary.com/${this.cloudName}/image/upload/${transformStr}${publicId}`;
  }

  /**
   * Get thumbnail URL for a file
   */
  getThumbnailUrl(publicId: string): string {
    return this.getOptimizedUrl(publicId, {
      width: 150,
      height: 150,
      crop: 'fill',
      quality: 'auto',
      format: 'webp'
    });
  }

  /**
   * Determine Cloudinary resource type from file MIME
   */
  private getResourceType(file: File): string {
    if (file.type.startsWith('image/')) return 'image';
    if (file.type.startsWith('video/')) return 'video';
    return 'raw'; // For PDFs, docs, etc.
  }
}
