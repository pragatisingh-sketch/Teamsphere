import { Component, OnInit, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import { FAQService, FAQ } from '../../../services/faq.service';
import { NotificationService } from '../../../shared/notification.service';
import { ConfirmationDialogComponent } from '../../../confirm-dialog/confirmation-dialog.component';

@Component({
    selector: 'app-faq-dialog',
    templateUrl: './faq-dialog.component.html',
    styleUrls: ['./faq-dialog.component.css']
})
export class FAQDialogComponent implements OnInit {
    faqs: FAQ[] = [];
    isLoading = true;
    canManage = false;
    editingFaq: FAQ | null = null;
    isEditing = false;

    // Form fields
    newQuestion = '';
    newAnswer = '';
    newCategory = 'General';
    newDisplayOrder = 0;

    categories = ['Troubleshooting', 'General', 'Support'];

    constructor(
        private faqService: FAQService,
        private notificationService: NotificationService,
        private dialog: MatDialog,
        public dialogRef: MatDialogRef<FAQDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: any
    ) { }

    ngOnInit(): void {
        this.loadFAQs();
        this.checkPermissions();
    }

    loadFAQs(): void {
        this.isLoading = true;
        this.faqService.getActiveFAQs().subscribe({
            next: (response) => {
                if (response.status === 'success') {
                    this.faqs = response.data;
                }
                this.isLoading = false;
            },
            error: () => {
                this.isLoading = false;
            }
        });
    }

    checkPermissions(): void {
        this.faqService.canManageFAQs().subscribe({
            next: (response) => {
                if (response.status === 'success') {
                    this.canManage = response.data.canManage;
                }
            }
        });
    }

    getGroupedFAQs(): { [key: string]: FAQ[] } {
        const grouped: { [key: string]: FAQ[] } = {};
        for (const faq of this.faqs) {
            const category = faq.category || 'General';
            if (!grouped[category]) {
                grouped[category] = [];
            }
            grouped[category].push(faq);
        }
        return grouped;
    }

    getCategoryIcon(category: string): string {
        switch (category) {
            case 'Troubleshooting': return 'build';
            case 'Support': return 'support_agent';
            default: return 'help_outline';
        }
    }

    startEdit(faq: FAQ): void {
        this.editingFaq = { ...faq };
        this.isEditing = true;
    }

    startCreate(): void {
        this.editingFaq = {
            question: '',
            answer: '',
            category: 'General',
            displayOrder: this.faqs.length + 1,
            isActive: true
        };
        this.isEditing = true;
    }

    cancelEdit(): void {
        this.editingFaq = null;
        this.isEditing = false;
    }

    saveFaq(): void {
        if (!this.editingFaq) return;

        if (!this.editingFaq.question || !this.editingFaq.answer) {
            this.notificationService.showWarning('Question and Answer are required');
            return;
        }

        const request = this.editingFaq.id
            ? this.faqService.updateFAQ(this.editingFaq.id, this.editingFaq)
            : this.faqService.createFAQ(this.editingFaq);

        request.subscribe({
            next: () => {
                this.notificationService.showSuccess(
                    this.editingFaq?.id ? 'FAQ updated successfully' : 'FAQ created successfully'
                );
                this.cancelEdit();
                this.loadFAQs();
            },
            error: (err) => {
                this.notificationService.showError(err.error?.message || 'Failed to save FAQ');
            }
        });
    }

    deleteFaq(faq: FAQ): void {
        const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
            width: '400px',
            data: {
                title: 'Delete FAQ',
                message: 'Are you sure you want to delete this FAQ?',
                confirmButtonText: 'Delete',
                color: 'warn'
            }
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result && faq.id) {
                this.faqService.deleteFAQ(faq.id).subscribe({
                    next: () => {
                        this.notificationService.showSuccess('FAQ deleted successfully');
                        this.loadFAQs();
                    },
                    error: (err) => {
                        this.notificationService.showError(err.error?.message || 'Failed to delete FAQ');
                    }
                });
            }
        });
    }

    initializeDefaults(): void {
        this.faqService.initializeDefaultFAQs().subscribe({
            next: () => {
                this.notificationService.showSuccess('Default FAQs initialized');
                this.loadFAQs();
            },
            error: (err) => {
                this.notificationService.showError(err.error?.message || 'Failed to initialize FAQs');
            }
        });
    }

    close(): void {
        this.dialogRef.close();
    }
}
