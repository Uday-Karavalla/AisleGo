import { api } from './client'

export interface Review {
  id: number
  reviewerName: string
  rating: number
  comment: string | null
  createdAt: string
  updatedAt: string
}

export interface StoreReviews {
  averageRating: number | null
  reviewCount: number
  reviews: Review[]
}

export interface MyReviewStatus {
  eligible: boolean
  myReview: Review | null
}

export interface SubmitReviewPayload {
  rating: number
  comment?: string
}

export const reviewsApi = {
  list: (storeId: string) => api.get<StoreReviews>(`/stores/${storeId}/reviews`),

  mine: (storeId: string) => api.get<MyReviewStatus>(`/stores/${storeId}/reviews/mine`),

  submit: (storeId: string, payload: SubmitReviewPayload) =>
    api.put<Review>(`/stores/${storeId}/reviews/mine`, payload),
}
