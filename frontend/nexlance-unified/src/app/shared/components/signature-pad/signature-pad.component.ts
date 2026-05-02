import {
  Component,
  ElementRef,
  ViewChild,
  AfterViewInit,
  Output,
  EventEmitter,
  Input,
  OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-signature-pad',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="signature-pad-wrapper">
      <canvas
        #canvas
        class="signature-canvas"
        (mousedown)="onMouseDown($event)"
        (mousemove)="onMouseMove($event)"
        (mouseup)="onMouseUp()"
        (mouseleave)="onMouseUp()"
        (touchstart)="onTouchStart($event)"
        (touchmove)="onTouchMove($event)"
        (touchend)="onMouseUp()"
      ></canvas>
      <div class="pad-actions">
        <button type="button" class="btn-clear" (click)="clear()">Clear</button>
      </div>
    </div>
  `,
  styles: [`
    .signature-pad-wrapper {
      border: 2px dashed #ccc;
      border-radius: 8px;
      background: #fafafa;
      padding: 4px;
    }
    .signature-canvas {
      width: 100%;
      height: 150px;
      display: block;
      cursor: crosshair;
      background: white;
      border-radius: 6px;
    }
    .pad-actions {
      display: flex;
      justify-content: flex-end;
      padding: 4px 0 0 0;
    }
    .btn-clear {
      background: none;
      border: 1px solid #ccc;
      padding: 4px 12px;
      border-radius: 4px;
      cursor: pointer;
      font-size: 12px;
      color: #666;
    }
    .btn-clear:hover { background: #f0f0f0; }
  `]
})
export class SignaturePadComponent implements AfterViewInit, OnDestroy {
  @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;
  @Output() signatureChange = new EventEmitter<string | null>();
  @Input() existingSignature: string | null = null;

  private ctx!: CanvasRenderingContext2D;
  private drawing = false;
  private hasContent = false;
  private resizeObserver!: ResizeObserver;

  ngAfterViewInit(): void {
    const canvas = this.canvasRef.nativeElement;
    this.ctx = canvas.getContext('2d')!;
    this.resizeCanvas();

    this.resizeObserver = new ResizeObserver(() => this.resizeCanvas());
    this.resizeObserver.observe(canvas.parentElement!);

    if (this.existingSignature) {
      this.loadImage(this.existingSignature);
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
  }

  private resizeCanvas(): void {
    const canvas = this.canvasRef.nativeElement;
    const rect = canvas.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    this.ctx.scale(dpr, dpr);
    this.ctx.lineWidth = 2;
    this.ctx.lineCap = 'round';
    this.ctx.lineJoin = 'round';
    this.ctx.strokeStyle = '#000';
    if (this.existingSignature) {
      this.loadImage(this.existingSignature);
    }
  }

  private loadImage(dataUrl: string): void {
    const img = new Image();
    img.onload = () => {
      const canvas = this.canvasRef.nativeElement;
      const rect = canvas.getBoundingClientRect();
      this.ctx.drawImage(img, 0, 0, rect.width, rect.height);
      this.hasContent = true;
    };
    img.src = dataUrl;
  }

  private getPos(e: MouseEvent): { x: number; y: number } {
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    return { x: e.clientX - rect.left, y: e.clientY - rect.top };
  }

  private getTouchPos(e: TouchEvent): { x: number; y: number } {
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    const touch = e.touches[0];
    return { x: touch.clientX - rect.left, y: touch.clientY - rect.top };
  }

  onMouseDown(e: MouseEvent): void {
    this.drawing = true;
    const pos = this.getPos(e);
    this.ctx.beginPath();
    this.ctx.moveTo(pos.x, pos.y);
  }

  onMouseMove(e: MouseEvent): void {
    if (!this.drawing) return;
    const pos = this.getPos(e);
    this.ctx.lineTo(pos.x, pos.y);
    this.ctx.stroke();
    this.hasContent = true;
  }

  onMouseUp(): void {
    if (this.drawing) {
      this.drawing = false;
      this.emitSignature();
    }
  }

  onTouchStart(e: TouchEvent): void {
    e.preventDefault();
    this.drawing = true;
    const pos = this.getTouchPos(e);
    this.ctx.beginPath();
    this.ctx.moveTo(pos.x, pos.y);
  }

  onTouchMove(e: TouchEvent): void {
    e.preventDefault();
    if (!this.drawing) return;
    const pos = this.getTouchPos(e);
    this.ctx.lineTo(pos.x, pos.y);
    this.ctx.stroke();
    this.hasContent = true;
  }

  private emitSignature(): void {
    if (this.hasContent) {
      const dataUrl = this.canvasRef.nativeElement.toDataURL('image/png');
      this.signatureChange.emit(dataUrl);
    }
  }

  clear(): void {
    const canvas = this.canvasRef.nativeElement;
    const rect = canvas.getBoundingClientRect();
    this.ctx.clearRect(0, 0, rect.width, rect.height);
    this.hasContent = false;
    this.signatureChange.emit(null);
  }

  getSignatureDataUrl(): string | null {
    return this.hasContent ? this.canvasRef.nativeElement.toDataURL('image/png') : null;
  }

  loadFromFile(file: File): void {
    const reader = new FileReader();
    reader.onload = (e) => {
      const dataUrl = e.target?.result as string;
      this.loadImage(dataUrl);
      this.signatureChange.emit(dataUrl);
    };
    reader.readAsDataURL(file);
  }
}
