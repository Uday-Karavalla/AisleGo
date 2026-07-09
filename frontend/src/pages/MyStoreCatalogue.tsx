import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { supermarketOwnerApi } from '../api/supermarket'
import type { NewOwnerBranch, NewOwnerProduct, OwnerBranch, OwnerProduct, UpdateOwnerProduct } from '../api/supermarket'
import { EmptyState } from '../components/EmptyState'
import { StoreIcon } from '../components/icons'

type Status = 'loading' | 'success' | 'error'

const EMPTY_BRANCH_DRAFT: NewOwnerBranch = {
  name: '',
  addressLine: '',
  city: '',
  latitude: 0,
  longitude: 0,
  openingTime: '09:00',
  closingTime: '21:00',
}

const EMPTY_PRODUCT_DRAFT = {
  name: '',
  description: '',
  sku: '',
  price: '',
  currency: 'INR',
  categoryName: '',
  initialStockQuantity: '0',
}

type EditProductDraft = {
  name: string
  description: string
  price: string
  currency: string
  categoryName: string
  imageUrl: string
  active: boolean
}

function toEditDraft(product: OwnerProduct): EditProductDraft {
  return {
    name: product.name,
    description: product.description ?? '',
    price: String(product.price),
    currency: product.currency,
    categoryName: product.categoryName ?? '',
    imageUrl: product.imageUrl ?? '',
    active: product.active,
  }
}

/** Self-service catalogue management: an owner sets up their branch (required before their
 *  store can ever appear in customer discovery - see `StoreDiscoveryService`) and manages
 *  products/stock from here. Creating a *new* branch/product is blocked until the owner's
 *  email is verified (stops a fake-email signup from ever reaching real customers); viewing
 *  and editing what already exists stays open regardless of admin-review status, so an owner
 *  can keep managing their catalogue while waiting on approval. */
export default function MyStoreCatalogue() {
  const { user } = useAuth()
  const [branches, setBranches] = useState<OwnerBranch[]>([])
  const [products, setProducts] = useState<OwnerProduct[]>([])
  const [status, setStatus] = useState<Status>('loading')
  const [message, setMessage] = useState<string | null>(null)

  const [branchDraft, setBranchDraft] = useState<NewOwnerBranch>(EMPTY_BRANCH_DRAFT)
  const [branchSubmitting, setBranchSubmitting] = useState(false)

  const [productDraft, setProductDraft] = useState(EMPTY_PRODUCT_DRAFT)
  const [productSubmitting, setProductSubmitting] = useState(false)

  const [stockEdits, setStockEdits] = useState<Record<number, string>>({})

  const [editingProductId, setEditingProductId] = useState<number | null>(null)
  const [editDraft, setEditDraft] = useState<EditProductDraft | null>(null)
  const [editSubmitting, setEditSubmitting] = useState(false)

  function load() {
    setStatus('loading')
    Promise.all([supermarketOwnerApi.listBranches(), supermarketOwnerApi.listProducts()])
      .then(([branchList, productList]) => {
        setBranches(branchList)
        setProducts(productList)
        setStatus('success')
      })
      .catch(() => setStatus('error'))
  }

  useEffect(load, [])

  async function handleCreateBranch(event: FormEvent) {
    event.preventDefault()
    setBranchSubmitting(true)
    setMessage(null)
    try {
      const branch = await supermarketOwnerApi.createBranch(branchDraft)
      setBranches((prev) => [...prev, branch])
      setBranchDraft(EMPTY_BRANCH_DRAFT)
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not create the branch.')
    } finally {
      setBranchSubmitting(false)
    }
  }

  async function handleCreateProduct(event: FormEvent) {
    event.preventDefault()
    if (branches.length === 0) return
    setProductSubmitting(true)
    setMessage(null)
    try {
      const payload: NewOwnerProduct = {
        name: productDraft.name,
        description: productDraft.description || undefined,
        sku: productDraft.sku,
        price: Number(productDraft.price),
        currency: productDraft.currency,
        categoryName: productDraft.categoryName || undefined,
        branchId: branches[0].id,
        initialStockQuantity: Number(productDraft.initialStockQuantity) || 0,
      }
      const product = await supermarketOwnerApi.createProduct(payload)
      setProducts((prev) => [...prev, product].sort((a, b) => a.name.localeCompare(b.name)))
      setProductDraft(EMPTY_PRODUCT_DRAFT)
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not add the product.')
    } finally {
      setProductSubmitting(false)
    }
  }

  async function handleToggleActive(product: OwnerProduct) {
    setMessage(null)
    try {
      const updated = await supermarketOwnerApi.updateProduct(product.id, {
        name: product.name,
        description: product.description ?? undefined,
        price: product.price,
        currency: product.currency,
        categoryName: product.categoryName ?? undefined,
        imageUrl: product.imageUrl ?? undefined,
        active: !product.active,
      })
      setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)))
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not update the product.')
    }
  }

  function startEditProduct(product: OwnerProduct) {
    setMessage(null)
    setEditingProductId(product.id)
    setEditDraft(toEditDraft(product))
  }

  function cancelEditProduct() {
    setEditingProductId(null)
    setEditDraft(null)
  }

  async function handleSaveProduct(event: FormEvent, product: OwnerProduct) {
    event.preventDefault()
    if (!editDraft) return
    setEditSubmitting(true)
    setMessage(null)
    try {
      const payload: UpdateOwnerProduct = {
        name: editDraft.name,
        description: editDraft.description || undefined,
        price: Number(editDraft.price),
        currency: editDraft.currency,
        categoryName: editDraft.categoryName || undefined,
        imageUrl: editDraft.imageUrl || undefined,
        active: editDraft.active,
      }
      const updated = await supermarketOwnerApi.updateProduct(product.id, payload)
      setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)))
      cancelEditProduct()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not update the product.')
    } finally {
      setEditSubmitting(false)
    }
  }

  async function handleSaveStock(product: OwnerProduct, branchId: number) {
    const raw = stockEdits[product.id]
    if (raw === undefined) return
    const quantity = Number(raw)
    if (Number.isNaN(quantity) || quantity < 0) return

    setMessage(null)
    try {
      const updated = await supermarketOwnerApi.updateInventory(product.id, branchId, quantity)
      setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)))
      setStockEdits((prev) => {
        const next = { ...prev }
        delete next[product.id]
        return next
      })
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not update stock.')
    }
  }

  if (status === 'loading') {
    return <div className="px-5 py-16 text-center text-sm text-ink-muted">Loading your catalogue…</div>
  }

  if (status === 'error') {
    return (
      <EmptyState
        icon={<StoreIcon className="h-12 w-12" />}
        title="Couldn't load your catalogue"
        description="Check your connection and try again."
      />
    )
  }

  return (
    <div className="flex flex-col gap-6 px-5 py-8">
      <div>
        <h1 className="text-xl font-extrabold text-ink">Manage your store</h1>
        <p className="mt-1 text-sm text-ink-muted">Set up your branch and manage the products customers will see.</p>
      </div>

      {message && (
        <p role="alert" className="text-sm text-danger-500">
          {message}
        </p>
      )}

      <section className="card flex flex-col gap-3">
        <h2 className="text-sm font-bold text-ink">Branch</h2>

        {branches.length === 0 && user && !user.emailVerified ? (
          <div className="flex flex-col gap-2 rounded-2xl bg-warning-50 p-3 text-sm text-warning-500">
            <p>Verify your email before setting up your branch — this keeps every live store genuine.</p>
            <Link to="/verify-email" className="btn-primary self-start px-4 py-2 text-xs">
              Verify email
            </Link>
          </div>
        ) : branches.length === 0 ? (
          <form onSubmit={handleCreateBranch} className="flex flex-col gap-2">
            <p className="text-sm text-ink-muted">
              Add your branch's location before adding products — customers can't discover your store without one.
            </p>
            <input
              className="input-field"
              placeholder="Branch name (e.g. Downtown)"
              value={branchDraft.name}
              onChange={(e) => setBranchDraft((d) => ({ ...d, name: e.target.value }))}
              required
            />
            <input
              className="input-field"
              placeholder="Address line"
              value={branchDraft.addressLine}
              onChange={(e) => setBranchDraft((d) => ({ ...d, addressLine: e.target.value }))}
            />
            <div className="flex gap-2">
              <input
                className="input-field"
                placeholder="City"
                value={branchDraft.city}
                onChange={(e) => setBranchDraft((d) => ({ ...d, city: e.target.value }))}
              />
            </div>
            <div className="flex gap-2">
              <input
                className="input-field"
                type="number"
                step="any"
                placeholder="Latitude"
                value={branchDraft.latitude || ''}
                onChange={(e) => setBranchDraft((d) => ({ ...d, latitude: Number(e.target.value) }))}
                required
              />
              <input
                className="input-field"
                type="number"
                step="any"
                placeholder="Longitude"
                value={branchDraft.longitude || ''}
                onChange={(e) => setBranchDraft((d) => ({ ...d, longitude: Number(e.target.value) }))}
                required
              />
            </div>
            <div className="flex gap-2">
              <input
                className="input-field"
                type="time"
                value={branchDraft.openingTime}
                onChange={(e) => setBranchDraft((d) => ({ ...d, openingTime: e.target.value }))}
              />
              <input
                className="input-field"
                type="time"
                value={branchDraft.closingTime}
                onChange={(e) => setBranchDraft((d) => ({ ...d, closingTime: e.target.value }))}
              />
            </div>
            <button type="submit" className="btn-primary" disabled={branchSubmitting}>
              {branchSubmitting ? 'Creating…' : 'Create branch'}
            </button>
          </form>
        ) : (
          branches.map((branch) => (
            <div key={branch.id} className="text-sm text-ink-muted">
              <span className="font-semibold text-ink">{branch.name}</span> — {branch.addressLine}, {branch.city} ·{' '}
              {branch.openingTime}–{branch.closingTime}
            </div>
          ))
        )}
      </section>

      {branches.length > 0 && (
        <section className="card flex flex-col gap-3">
          <h2 className="text-sm font-bold text-ink">Add a product</h2>
          <form onSubmit={handleCreateProduct} className="flex flex-col gap-2">
            <input
              className="input-field"
              placeholder="Product name"
              value={productDraft.name}
              onChange={(e) => setProductDraft((d) => ({ ...d, name: e.target.value }))}
              required
            />
            <input
              className="input-field"
              placeholder="Description (optional)"
              value={productDraft.description}
              onChange={(e) => setProductDraft((d) => ({ ...d, description: e.target.value }))}
            />
            <div className="flex gap-2">
              <input
                className="input-field"
                placeholder="SKU"
                value={productDraft.sku}
                onChange={(e) => setProductDraft((d) => ({ ...d, sku: e.target.value }))}
                required
              />
              <input
                className="input-field"
                placeholder="Category (optional)"
                value={productDraft.categoryName}
                onChange={(e) => setProductDraft((d) => ({ ...d, categoryName: e.target.value }))}
              />
            </div>
            <div className="flex gap-2">
              <input
                className="input-field"
                type="number"
                step="0.01"
                min="0"
                placeholder="Price"
                value={productDraft.price}
                onChange={(e) => setProductDraft((d) => ({ ...d, price: e.target.value }))}
                required
              />
              <input
                className="input-field"
                type="number"
                min="0"
                placeholder="Initial stock"
                value={productDraft.initialStockQuantity}
                onChange={(e) => setProductDraft((d) => ({ ...d, initialStockQuantity: e.target.value }))}
              />
            </div>
            <button type="submit" className="btn-primary" disabled={productSubmitting}>
              {productSubmitting ? 'Adding…' : 'Add product'}
            </button>
          </form>
        </section>
      )}

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-bold text-ink">Your products ({products.length})</h2>

        {products.length === 0 && <p className="text-sm text-ink-muted">No products yet — add your first one above.</p>}

        {products.map((product) =>
          editingProductId === product.id && editDraft ? (
            <form
              key={product.id}
              onSubmit={(e) => handleSaveProduct(e, product)}
              className="card flex flex-col gap-2"
            >
              <input
                className="input-field"
                placeholder="Product name"
                value={editDraft.name}
                onChange={(e) => setEditDraft((d) => (d ? { ...d, name: e.target.value } : d))}
                required
              />
              <input
                className="input-field"
                placeholder="Description (optional)"
                value={editDraft.description}
                onChange={(e) => setEditDraft((d) => (d ? { ...d, description: e.target.value } : d))}
              />
              <div className="flex gap-2">
                <input
                  className="input-field"
                  placeholder="Category (optional)"
                  value={editDraft.categoryName}
                  onChange={(e) => setEditDraft((d) => (d ? { ...d, categoryName: e.target.value } : d))}
                />
                <input
                  className="input-field"
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="Price"
                  value={editDraft.price}
                  onChange={(e) => setEditDraft((d) => (d ? { ...d, price: e.target.value } : d))}
                  required
                />
              </div>
              <input
                className="input-field"
                placeholder="Image URL (optional)"
                value={editDraft.imageUrl}
                onChange={(e) => setEditDraft((d) => (d ? { ...d, imageUrl: e.target.value } : d))}
              />
              <label className="flex items-center gap-2 text-xs text-ink-muted">
                <input
                  type="checkbox"
                  checked={editDraft.active}
                  onChange={(e) => setEditDraft((d) => (d ? { ...d, active: e.target.checked } : d))}
                />
                Active (visible to customers)
              </label>
              <div className="flex gap-2">
                <button type="submit" className="btn-primary flex-1" disabled={editSubmitting}>
                  {editSubmitting ? 'Saving…' : 'Save changes'}
                </button>
                <button type="button" className="btn-secondary flex-1" onClick={cancelEditProduct}>
                  Cancel
                </button>
              </div>
            </form>
          ) : (
            <div key={product.id} className="card flex flex-col gap-2">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <p className="text-sm font-semibold text-ink">{product.name}</p>
                  <p className="text-xs text-ink-faint">
                    {product.categoryName ?? 'Uncategorized'} · ₹{product.price.toFixed(2)}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => startEditProduct(product)}
                    className="rounded-full bg-surface-muted px-3 py-1 text-xs font-semibold text-ink"
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    onClick={() => handleToggleActive(product)}
                    className={`rounded-full px-3 py-1 text-xs font-semibold ${
                      product.active ? 'bg-brand-50 text-brand-700' : 'bg-danger-50 text-danger-500'
                    }`}
                  >
                    {product.active ? 'Active' : 'Inactive'}
                  </button>
                </div>
              </div>

              {product.branchStock.map((stock) => (
                <div key={stock.branchId} className="flex items-center gap-2 text-xs">
                  <span className="text-ink-muted">{stock.branchName} stock:</span>
                  <input
                    className="input-field w-20 py-1 text-xs"
                    type="number"
                    min="0"
                    value={stockEdits[product.id] ?? stock.quantityOnHand}
                    onChange={(e) => setStockEdits((prev) => ({ ...prev, [product.id]: e.target.value }))}
                  />
                  <button
                    type="button"
                    className="btn-secondary px-3 py-1 text-xs"
                    onClick={() => handleSaveStock(product, stock.branchId)}
                  >
                    Save
                  </button>
                </div>
              ))}
            </div>
          ),
        )}
      </section>
    </div>
  )
}
