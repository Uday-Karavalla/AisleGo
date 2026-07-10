import { api, ApiError } from './client'
import { FIXTURE_PRODUCTS } from './fixtures'

export interface Product {
  id: string
  storeId: string
  name: string
  description?: string
  imageUrl?: string
  /** Selling price in the store's local currency (minor-unit-free, e.g. 58.00). */
  price: number
  /** Maximum retail price, if discounted. */
  mrp?: number
  unit: string
  category: string
  inStock: boolean
  stockQuantity?: number
}

export interface ProductListResponse {
  products: Product[]
  page: number
  pageSize: number
  totalCount: number
  totalPages: number
}

export interface ProductListParams {
  storeId: string
  search?: string
  category?: string
  page?: number
  pageSize?: number
}

function paginateFixtures(params: ProductListParams): ProductListResponse {
  const page = params.page ?? 1
  const pageSize = params.pageSize ?? 20
  let filtered = FIXTURE_PRODUCTS.filter((product) => product.storeId === params.storeId)
  if (params.category) {
    filtered = filtered.filter((product) => product.category === params.category)
  }
  if (params.search) {
    const term = params.search.toLowerCase()
    filtered = filtered.filter((product) => product.name.toLowerCase().includes(term))
  }
  const start = (page - 1) * pageSize
  const pageItems = filtered.slice(start, start + pageSize)
  return {
    products: pageItems,
    page,
    pageSize,
    totalCount: filtered.length,
    totalPages: Math.max(1, Math.ceil(filtered.length / pageSize)),
  }
}

/** Raw shape of one element in GET /api/stores/{id}/products' `content` array (see backend's `ProductResponse`). */
interface RawProduct {
  id: number
  supermarketId: number
  name: string
  description: string | null
  sku: string
  price: number
  currency: string
  categoryName: string | null
  imageUrl: string | null
}

/** Raw shape of GET /api/stores/{id}/products - a Spring Data `Page<ProductResponse>`, not the
 *  `{ products, totalCount }` shape this module exposes to the rest of the app. `pageNumber` is
 *  0-indexed, unlike this module's `page`, which is 1-indexed throughout the UI. */
interface RawProductPage {
  content: RawProduct[]
  number: number
  totalPages: number
  totalElements: number
}

function fromRawProduct(raw: RawProduct, storeId: string): Product {
  return {
    id: String(raw.id),
    storeId,
    name: raw.name,
    description: raw.description ?? undefined,
    imageUrl: raw.imageUrl ?? undefined,
    price: raw.price,
    unit: '',
    category: raw.categoryName ?? '',
    // The listing endpoint doesn't expose stock - availability is only enforced at checkout
    // (see InventoryReservationService), so every listed product is treated as in stock here.
    inStock: true,
  }
}

/** One item in a cross-store category browse result (see `GET /api/stores/category-products`) -
 *  unlike {@link Product}, tagged with which store it's from and the nearest branch to fulfil
 *  it from, since results here mix products across every nearby supermarket. */
export interface CategoryProduct {
  id: string
  name: string
  description?: string
  imageUrl?: string
  price: number
  currency: string
  categoryName: string
  supermarketId: string
  supermarketName: string
  branchId: string
  distanceKm: number
}

export interface CategoryProductListParams {
  /** Matched by substring against the shared category name (e.g. "vegetable" matches the
   *  seeded "Fruits & Vegetables" category) - there's no fixed category taxonomy, see
   *  ProductRepository#searchByCategoryKeywordAcrossSupermarkets. */
  category: string
  lat: number
  lng: number
  radiusKm?: number
  page?: number
  pageSize?: number
}

export interface CategoryProductListResponse {
  products: CategoryProduct[]
  page: number
  totalPages: number
  totalCount: number
}

interface RawCategoryProduct {
  id: number
  name: string
  description: string | null
  sku: string
  price: number
  currency: string
  categoryName: string | null
  imageUrl: string | null
  supermarketId: number
  supermarketName: string
  branchId: number
  distanceKm: number
}

interface RawCategoryProductPage {
  content: RawCategoryProduct[]
  number: number
  totalPages: number
  totalElements: number
}

export const productsApi = {
  async list(params: ProductListParams): Promise<ProductListResponse> {
    const pageSize = params.pageSize ?? 20
    const query = new URLSearchParams({
      page: String((params.page ?? 1) - 1),
      size: String(pageSize),
      ...(params.search ? { search: params.search } : {}),
      ...(params.category ? { category: params.category } : {}),
    })
    try {
      const response = await api.get<RawProductPage>(`/stores/${params.storeId}/products?${query.toString()}`)
      return {
        products: response.content.map((raw) => fromRawProduct(raw, params.storeId)),
        page: response.number + 1,
        pageSize,
        totalCount: response.totalElements,
        totalPages: response.totalPages,
      }
    } catch (error) {
      if (error instanceof ApiError && error.isNetworkError) {
        return paginateFixtures(params)
      }
      throw error
    }
  },

  async categories(storeId: string): Promise<string[]> {
    try {
      const response = await api.get<{ categories: string[] }>(`/stores/${storeId}/categories`)
      return response.categories
    } catch (error) {
      if (error instanceof ApiError && error.isNetworkError) {
        const categories = new Set(
          FIXTURE_PRODUCTS.filter((product) => product.storeId === storeId).map((product) => product.category),
        )
        return Array.from(categories)
      }
      throw error
    }
  },

  async byCategory(params: CategoryProductListParams): Promise<CategoryProductListResponse> {
    const query = new URLSearchParams({
      category: params.category,
      lat: String(params.lat),
      lng: String(params.lng),
      radiusKm: String(params.radiusKm ?? 10),
      page: String((params.page ?? 1) - 1),
      size: String(params.pageSize ?? 20),
    })
    const response = await api.get<RawCategoryProductPage>(`/stores/category-products?${query.toString()}`)
    return {
      products: response.content.map((raw) => ({
        id: String(raw.id),
        name: raw.name,
        description: raw.description ?? undefined,
        imageUrl: raw.imageUrl ?? undefined,
        price: raw.price,
        currency: raw.currency,
        categoryName: raw.categoryName ?? '',
        supermarketId: String(raw.supermarketId),
        supermarketName: raw.supermarketName,
        branchId: String(raw.branchId),
        distanceKm: raw.distanceKm,
      })),
      page: response.number + 1,
      totalPages: response.totalPages,
      totalCount: response.totalElements,
    }
  },
}
