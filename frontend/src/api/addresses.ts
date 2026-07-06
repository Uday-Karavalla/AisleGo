import { api, ApiError } from './client'

export interface Address {
  id: string
  label: string
  line1: string
  line2?: string
  city: string
  state: string
  postalCode: string
  lat?: number
  lng?: number
  isDefault?: boolean
}

export type NewAddress = Omit<Address, 'id'>

const LOCAL_ADDRESSES_KEY = 'aislego.addresses'

function readLocalAddresses(): Address[] {
  try {
    const raw = localStorage.getItem(LOCAL_ADDRESSES_KEY)
    return raw ? (JSON.parse(raw) as Address[]) : []
  } catch {
    return []
  }
}

function writeLocalAddresses(addresses: Address[]): void {
  try {
    localStorage.setItem(LOCAL_ADDRESSES_KEY, JSON.stringify(addresses))
  } catch {
    // ignore persistence failures (private mode / storage full)
  }
}

/**
 * Addresses are backed by the API, but fall back to a local cache when the
 * backend isn't reachable yet so Checkout stays usable during development.
 */
export const addressesApi = {
  async list(): Promise<Address[]> {
    try {
      return await api.get<Address[]>('/addresses')
    } catch (error) {
      if (error instanceof ApiError && error.isNetworkError) {
        return readLocalAddresses()
      }
      throw error
    }
  },

  async create(address: NewAddress): Promise<Address> {
    try {
      return await api.post<Address>('/addresses', address)
    } catch (error) {
      if (error instanceof ApiError && error.isNetworkError) {
        const saved: Address = { ...address, id: crypto.randomUUID() }
        const existing = readLocalAddresses()
        writeLocalAddresses([...existing, saved])
        return saved
      }
      throw error
    }
  },
}
