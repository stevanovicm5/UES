import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment.development';
import { AddVenueRequest } from '../../models/venue/AddVenueRequest';
import { Observable } from 'rxjs';
import { FilterVenueDto } from '../../models/venue/FilterVenueDto';
import { UpdateVenueDto } from '../../models/venue/UpdateVenueDto';

@Injectable({
  providedIn: 'root',
})
export class VenueService {
  constructor(private http: HttpClient) {}

  public addVenue(venueData: AddVenueRequest, venueImage: File) {
    let formData = new FormData();
    formData.append('venue', JSON.stringify(venueData));
    formData.append('image', venueImage);

    return this.http.post(environment.apiUrl + '/venue', formData);
  }

  public filterVenues(
    filterVenueDto: FilterVenueDto,
    pageNumber: number
  ): Observable<any> {
    let venueTypeParam = null;

    if (filterVenueDto.venueType != null) {
      venueTypeParam = filterVenueDto.venueType;
    }

    let queryParameters =
      '?filter=' +
      filterVenueDto.filter +
      '&page=' +
      pageNumber.toString() +
      '&size=8';
    if (filterVenueDto.venueType != null) {
      queryParameters += '&venueType=' + filterVenueDto.venueType.toString();
    }
    
    return this.http.get(environment.apiUrl + '/venue' + queryParameters);
  }

  public findVenueById(venueId: string) : Observable<any> {
    return this.http.get(`${environment.apiUrl}/venue/${venueId}`);
  }

  public updateVenue(venueId: string, body: UpdateVenueDto) : Observable<any> {
    return this.http.put(`${environment.apiUrl}/venue/${venueId}`, body)
  }

  public deleteVenue(venueId: string) : Observable<any> {
    return this.http.delete(`${environment.apiUrl}/venue/${venueId}`);
  }

  public getTopVenues(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/venue/top`);
  }

  // ========== UES SEARCH METHODS ==========

  public advancedSearch(params: any, page: number, sortField?: string, sortDir?: string): Observable<any> {
    let queryParams = `?page=${page}&size=8`;

    if (params.name) queryParams += `&name=${encodeURIComponent(params.name)}`;
    if (params.description) queryParams += `&description=${encodeURIComponent(params.description)}`;
    if (params.pdfDescription) queryParams += `&pdfDescription=${encodeURIComponent(params.pdfDescription)}`;
    if (params.minReviews != null) queryParams += `&minReviews=${params.minReviews}`;
    if (params.maxReviews != null) queryParams += `&maxReviews=${params.maxReviews}`;
    if (params.minRating != null) queryParams += `&minRating=${params.minRating}`;
    if (params.maxRating != null) queryParams += `&maxRating=${params.maxRating}`;
    if (params.ratingCategory) queryParams += `&ratingCategory=${params.ratingCategory}`;
    if (params.operator) queryParams += `&operator=${params.operator}`;
    if (sortField) queryParams += `&sort=${sortField},${sortDir || 'asc'}`;

    return this.http.get(`${environment.apiUrl}/venue/search${queryParams}`);
  }

  public findSimilarVenues(venueId: number): Observable<any> {
    return this.http.get(`${environment.apiUrl}/venue/similar/${venueId}`);
  }

  public uploadVenuePdf(venueId: number, pdfFile: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', pdfFile);
    return this.http.post(`${environment.apiUrl}/venue/${venueId}/pdf`, formData, {
      responseType: 'text'
    });
  }

  public downloadVenuePdf(venueId: number): Observable<Blob> {
    return this.http.get(`${environment.apiUrl}/venue/${venueId}/pdf`, {
      responseType: 'blob'
    });
  }
}
