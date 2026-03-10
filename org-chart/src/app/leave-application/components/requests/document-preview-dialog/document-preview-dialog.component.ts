import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'app-document-preview-dialog',
  templateUrl: './document-preview-dialog.component.html'
})
export class DocumentPreviewDialogComponent {
  fullDocumentPath: string;

  constructor(@Inject(MAT_DIALOG_DATA) public data: any) {
    // If full URL is already passed, use it. If it's relative, add base
    const baseUrl = 'https://teamsphere.in/'; // Change for local if needed

    this.fullDocumentPath = data.documentPath?.startsWith('http')
      ? data.documentPath
      : baseUrl + data.documentPath;
  }

  isImage(path: string): boolean {
    return /\.(jpg|jpeg|png|gif)$/i.test(path || '');
  }

  getFilenameFromPath(path: string): string {
    return path?.split('/').pop() || 'document';
  }
}

