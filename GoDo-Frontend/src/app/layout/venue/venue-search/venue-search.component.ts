import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { SelectModule } from 'primeng/select';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { FloatLabelModule } from 'primeng/floatlabel';
import { InputNumberModule } from 'primeng/inputnumber';
import { VenueService } from '../../../services/venue/venue.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-venue-search',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InputTextModule,
    ButtonModule,
    SelectModule,
    PaginatorModule,
    FloatLabelModule,
    InputNumberModule
  ],
  templateUrl: './venue-search.component.html',
  styleUrl: './venue-search.component.css',
})
export class VenueSearchComponent implements OnInit {
  results: any[] = [];
  totalElements: number = 0;
  rows: number = 8;
  first: number = 0;
  currentPage: number = 0;
  currentSortField: string = '';
  currentSortDir: string = 'asc';

  ratingCategories = [
    { name: 'Average Rating', value: 'average' },
    { name: 'Performance', value: 'performance' },
    { name: 'Ambient', value: 'ambient' },
    { name: 'Venue', value: 'venue' },
    { name: 'Overall Impression', value: 'overallImpression' },
  ];

  operators = [
    { name: 'AND', value: 'AND' },
    { name: 'OR', value: 'OR' },
  ];

  sortOptions = [
    { name: 'Default (Relevance)', value: '' },
    { name: 'Name A-Z', value: 'asc' },
    { name: 'Name Z-A', value: 'desc' },
  ];

  searchForm = new FormGroup({
    name: new FormControl(''),
    description: new FormControl(''),
    pdfDescription: new FormControl(''),
    minReviews: new FormControl(null),
    maxReviews: new FormControl(null),
    minRating: new FormControl(null),
    maxRating: new FormControl(null),
    ratingCategory: new FormControl(this.ratingCategories[0]),
    operator: new FormControl(this.operators[0]),
    sort: new FormControl(this.sortOptions[0]),
  });

  constructor(
    private venueService: VenueService,
    private router: Router
  ) {}

  ngOnInit(): void {}

  search() {
    this.first = 0;
    this.currentPage = 0;
    this.executeSearch();
  }

  executeSearch() {
    const formValues = this.searchForm.value;

    const params: any = {};
    if (formValues.name?.trim()) params.name = formValues.name.trim();
    if (formValues.description?.trim()) params.description = formValues.description.trim();
    if (formValues.pdfDescription?.trim()) params.pdfDescription = formValues.pdfDescription.trim();
    if (formValues.minReviews != null) params.minReviews = formValues.minReviews;
    if (formValues.maxReviews != null) params.maxReviews = formValues.maxReviews;
    if (formValues.minRating != null) params.minRating = formValues.minRating;
    if (formValues.maxRating != null) params.maxRating = formValues.maxRating;
    if (formValues.ratingCategory?.value && formValues.ratingCategory.value !== 'average') {
      params.ratingCategory = formValues.ratingCategory.value;
    }
    params.operator = formValues.operator?.value || 'AND';

    const sortVal = formValues.sort?.value;
    if (sortVal) {
      this.currentSortField = 'name';
      this.currentSortDir = sortVal;
    } else {
      this.currentSortField = '';
      this.currentSortDir = '';
    }

    this.venueService
      .advancedSearch(params, this.currentPage, this.currentSortField, this.currentSortDir)
      .subscribe({
        next: (response) => {
          this.results = response.content;
          this.totalElements = response.totalElements;
        },
        error: (error) => {
          console.error('Search failed', error);
        },
      });
  }

  onPageChange($event: PaginatorState) {
    this.first = $event.first ?? 0;
    this.currentPage = $event.page ?? 0;
    this.executeSearch();
  }

  clearForm() {
    this.searchForm.reset({
      name: '',
      description: '',
      pdfDescription: '',
      minReviews: null,
      maxReviews: null,
      minRating: null,
      maxRating: null,
      ratingCategory: this.ratingCategories[0],
      operator: this.operators[0],
      sort: this.sortOptions[0],
    });
    this.results = [];
    this.totalElements = 0;
  }

  goToVenue(venueId: number) {
    this.router.navigate(['/venue', venueId]);
  }

  getTypeName(type: string): string {
    const map: Record<string, string> = {
      'CULTURAL_CENTER': 'Cultural Center',
      'BAR': 'Bar',
      'NIGHT_CLUB': 'Night Club',
      'RESTAURANT': 'Restaurant',
      'THEATER': 'Theater',
      'ROOFTOP': 'Rooftop',
      'STADIUM': 'Stadium',
      'MUSEUM': 'Museum',
    };
    return map[type] ?? type;
  }

  getTypeColor(type: string): string {
    const colors: Record<string, string> = {
      'RESTAURANT': '#AEEA00',
      'BAR': '#FF9100',
      'NIGHT_CLUB': '#F50057',
      'THEATER': '#FF3D00',
      'STADIUM': '#00E5FF',
      'CULTURAL_CENTER': '#1DE9B6',
      'MUSEUM': '#FFAB00',
      'ROOFTOP': '#FFD600',
    };
    return colors[type] ?? '#6c757d';
  }

  downloadPdf(venueId: number, event: Event) {
    event.stopPropagation();
    this.venueService.downloadVenuePdf(venueId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `venue_${venueId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Failed to download PDF', error);
      },
    });
  }
}
