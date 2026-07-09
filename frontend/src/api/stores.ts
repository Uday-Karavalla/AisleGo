import { api, ApiError } from './client'
import { FIXTURE_STORES } from './fixtures'

export interface Store {
  id: string
  name: string
  logoUrl?: string
  /**
   * The supermarket this branch (`id`) belongs to - products, categories and reviews are
   * fetched by this, not by `id`, since the catalogue is shared across a supermarket's
   * branches. Undefined only for the offline fixture fallback, which has no real backend
   * ids to resolve; callers should fall back to `id` in that case.
   */
  supermarketId?: string
  /** 0-5 average from `GET /api/stores/{id}/reviews`'s aggregate; absent when the store has no reviews yet. */
  rating?: number
  ratingCount?: number
  distanceKm: number
  /** Real routing-provider ETA when configured, great-circle estimate otherwise. */
  etaMinutes: number
  isOpen: boolean
  address: string
  /** Optional: no backing schema on the backend yet, fixtures only. */
  categories?: string[]
}

/** Raw shape of each element returned by GET /api/stores/nearby (a bare array). */
interface NearbyBranch {
  branchId: number
  branchName: string
  addressLine: string | null
  city: string | null
  latitude: number
  longitude: number
  supermarketId: number
  supermarketName: string
  distanceKm: number
  etaMinutes: number
  isOpen: boolean
  rating: number | null
  ratingCount: number
}

interface GeocodeResponse {
  lat: number
  lng: number
}

export interface NearbyStoresParams {
  lat: number
  lng: number
  radiusKm?: number
}

function combineAddress(addressLine: string | null, city: string | null): string {
  return [addressLine, city].filter((part): part is string => Boolean(part && part.trim())).join(', ')
}

function fromNearbyBranch(branch: NearbyBranch): Store {
  return {
    // Deliberately branchId, not supermarketId: Checkout.tsx reads cart.storeId straight back
    // as the branchId to order from.
    id: String(branch.branchId),
    supermarketId: String(branch.supermarketId),
    name: branch.branchName,
    address: combineAddress(branch.addressLine, branch.city),
    distanceKm: branch.distanceKm,
    etaMinutes: branch.etaMinutes,
    isOpen: branch.isOpen,
    rating: branch.rating ?? undefined,
    ratingCount: branch.ratingCount,
  }
}

/** Raw shape of GET /api/stores/branches/{branchId} - resolves one branch by id, which is
 *  what the storefront route navigates by. */
interface BranchDetail {
  branchId: number
  branchName: string
  addressLine: string | null
  city: string | null
  isOpen: boolean
  supermarketId: number
  supermarketName: string
  logoUrl: string | null
  rating: number | null
  ratingCount: number
}

function fromBranchDetail(branch: BranchDetail): Store {
  return {
    id: String(branch.branchId),
    supermarketId: String(branch.supermarketId),
    name: branch.branchName,
    logoUrl: branch.logoUrl ?? undefined,
    address: combineAddress(branch.addressLine, branch.city),
    distanceKm: 0,
    etaMinutes: 0,
    isOpen: branch.isOpen,
    rating: branch.rating ?? undefined,
    ratingCount: branch.ratingCount,
  }
}

export const storesApi = {
  async nearby(params: NearbyStoresParams): Promise<Store[]> {
    const query = new URLSearchParams({
      lat: String(params.lat),
      lng: String(params.lng),
      ...(params.radiusKm ? { radiusKm: String(params.radiusKm) } : {}),
    })
    try {
      const branches = await api.get<NearbyBranch[]>(`/stores/nearby?${query.toString()}`)
      return branches.map(fromNearbyBranch)
    } catch (error) {
      if (error instanceof ApiError && error.isNetworkError) {
        return FIXTURE_STORES
      }
      throw error
    }
  },

  async getById(storeId: string): Promise<Store> {
    try {
      const branch = await api.get<BranchDetail>(`/stores/branches/${storeId}`)
      return fromBranchDetail(branch)
    } catch (error) {
      if (error instanceof ApiError && error.isNetworkError) {
        const fixture = FIXTURE_STORES.find((store) => store.id === storeId)
        if (fixture) return fixture
      }
      throw error
    }
  },

  /**
   * Resolves free-text address input to coordinates via GET /api/stores/geocode.
   * Returns null when the address can't be resolved (404 — either the active
   * routing provider doesn't support geocoding at all, or it found no match),
   * which callers should treat as "couldn't find that address," not an error.
   * Genuine network/5xx failures are propagated so callers can show a retry path.
   */
  async geocode(query: string): Promise<{ lat: number; lng: number } | null> {
    try {
      return await api.get<GeocodeResponse>(`/stores/geocode?query=${encodeURIComponent(query)}`)
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        return null
      }
      throw error
    }
  },
}
