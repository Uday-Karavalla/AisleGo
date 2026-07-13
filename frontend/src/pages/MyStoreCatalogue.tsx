import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { supermarketOwnerApi } from '../api/supermarket'
import { storesApi } from '../api/stores'
import type {
  NewOwnerBranch,
  NewOwnerProduct,
  OwnerBranch,
  OwnerProduct,
  UpdateOwnerBranch,
  UpdateOwnerProduct,
  MySupermarket,
  OwnerInsights,
} from '../api/supermarket'
import { EmptyState } from '../components/EmptyState'
import { StoreIcon } from '../components/icons'
import { Dialog } from '../components/Dialog'
import { CouponManager } from '../components/CouponManager'
import { ownerCouponApi } from '../api/coupons'

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
  imageUrl: '',
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

function toEditProductDraft(product: OwnerProduct): EditProductDraft {
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

type EditBranchDraft = NewOwnerBranch & { active: boolean }

function toEditBranchDraft(branch: OwnerBranch): EditBranchDraft {
  return {
    name: branch.name,
    addressLine: branch.addressLine ?? '',
    city: branch.city ?? '',
    latitude: branch.latitude,
    longitude: branch.longitude,
    openingTime: branch.openingTime ?? '09:00',
    closingTime: branch.closingTime ?? '21:00',
    active: branch.active,
  }
}

/** Self-service catalogue management: an owner sets up their branch(es) (required before their
 *  store can ever appear in customer discovery - see `StoreDiscoveryService`) and manages
 *  products/stock from here. Creating a *new* branch/product is blocked until the owner's
 *  email is verified (stops a fake-email signup from ever reaching real customers); viewing
 *  and editing what already exists stays open regardless of admin-review status, so an owner
 *  can keep managing their catalogue while waiting on approval. */
export default function MyStoreCatalogue() {
  const { user } = useAuth()
  const [branches, setBranches] = useState<OwnerBranch[]>([])
  const [products, setProducts] = useState<OwnerProduct[]>([])
  const [supermarket, setSupermarket] = useState<MySupermarket | null>(null)
  const [insights, setInsights] = useState<OwnerInsights | null>(null)
  const [status, setStatus] = useState<Status>('loading')
  const [message, setMessage] = useState<string | null>(null)

  const [showBranchForm, setShowBranchForm] = useState(false)
  const [branchDraft, setBranchDraft] = useState<NewOwnerBranch>(EMPTY_BRANCH_DRAFT)
  const [branchSubmitting, setBranchSubmitting] = useState(false)

  const [editingBranchId, setEditingBranchId] = useState<number | null>(null)
  const [editBranchDraft, setEditBranchDraft] = useState<EditBranchDraft | null>(null)
  const [editBranchSubmitting, setEditBranchSubmitting] = useState(false)
  const [deleteBranchTarget, setDeleteBranchTarget] = useState<OwnerBranch | null>(null)
  const [deletingBranch, setDeletingBranch] = useState(false)
  const [geocodingBranch, setGeocodingBranch] = useState(false)

  const [productDraft, setProductDraft] = useState(EMPTY_PRODUCT_DRAFT)
  const [productBranchId, setProductBranchId] = useState<number | null>(null)
  const [productSubmitting, setProductSubmitting] = useState(false)

  const [stockEdits, setStockEdits] = useState<Record<number, string>>({})

  const [editingProductId, setEditingProductId] = useState<number | null>(null)
  const [editDraft, setEditDraft] = useState<EditProductDraft | null>(null)
  const [editSubmitting, setEditSubmitting] = useState(false)
  const [deleteProductTarget, setDeleteProductTarget] = useState<OwnerProduct | null>(null)
  const [deletingProduct, setDeletingProduct] = useState(false)
  const [bulkBranchId, setBulkBranchId] = useState<number | null>(null)
  const [bulkImporting, setBulkImporting] = useState(false)

  function load() {
    setStatus('loading')
    Promise.all([supermarketOwnerApi.listBranches(), supermarketOwnerApi.listProducts(), supermarketOwnerApi.mine()])
      .then(([branchList, productList, mine]) => {
        setBranches(branchList)
        setProducts(productList)
        setStatus('success')
        setProductBranchId((prev) => prev ?? branchList[0]?.id ?? null)
        setBulkBranchId((prev) => prev ?? branchList[0]?.id ?? null)
        setSupermarket(mine)
      })
      .catch(() => setStatus('error'))
  }

  useEffect(load, [])
  useEffect(() => { supermarketOwnerApi.insights().then(setInsights).catch(() => {}) }, [])

  async function handleCsvImport(file: File) {
    if (!bulkBranchId) { setMessage('Choose a branch before importing products.'); return }
    setBulkImporting(true)
    setMessage(null)
    try {
      const rows = (await file.text()).split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
      if (rows.length < 2) throw new Error('The CSV needs a header row and at least one product.')
      const header = rows[0].split(',').map((value) => value.trim().toLowerCase())
      const required = ['name', 'sku', 'price']
      if (required.some((column) => !header.includes(column))) throw new Error('CSV headers must include name, sku and price.')
      const productsToImport: NewOwnerProduct[] = rows.slice(1).map((line, index) => {
        const values = line.split(',').map((value) => value.trim().replace(/^"|"$/g, ''))
        const value = (column: string) => values[header.indexOf(column)] ?? ''
        const price = Number(value('price'))
        const stock = Number(value('stock') || 0)
        if (!value('name') || !value('sku') || !Number.isFinite(price) || price <= 0) throw new Error(`Invalid product on CSV row ${index + 2}.`)
        return { name: value('name'), description: value('description'), sku: value('sku'), price, currency: value('currency') || 'INR', categoryName: value('category'), imageUrl: value('imageurl'), branchId: bulkBranchId, initialStockQuantity: Number.isFinite(stock) ? Math.max(0, Math.floor(stock)) : 0 }
      })
      const result = await supermarketOwnerApi.importProducts(productsToImport)
      setProducts((current) => [...current, ...result.products].sort((a, b) => a.name.localeCompare(b.name)))
      setMessage(`${result.importedCount} products imported successfully.`)
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not import that CSV file.')
    } finally {
      setBulkImporting(false)
    }
  }

  async function handleCreateBranch(event: FormEvent) {
    event.preventDefault()
    setBranchSubmitting(true)
    setMessage(null)
    try {
      const branch = await supermarketOwnerApi.createBranch(branchDraft)
      setBranches((prev) => [...prev, branch])
      setProductBranchId((prev) => prev ?? branch.id)
      setBranchDraft(EMPTY_BRANCH_DRAFT)
      setShowBranchForm(false)
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not create the branch.')
    } finally {
      setBranchSubmitting(false)
    }
  }

  /** Resolves the typed address/city into real coordinates via the routing provider's
   *  geocoder, instead of leaving owners to type latitude/longitude by hand - manual entry
   *  is how a branch ends up thousands of km from where it actually is, invisible to every
   *  "nearby stores" search that has nothing to do with the store's real location. */
  async function handleLocateAddress(
    draft: NewOwnerBranch,
    setDraft: (updater: (d: NewOwnerBranch) => NewOwnerBranch) => void,
  ) {
    const query = [draft.addressLine, draft.city].filter((part) => part.trim()).join(', ')
    if (!query) {
      setMessage('Enter an address and city first, then locate coordinates.')
      return
    }
    setMessage(null)
    setGeocodingBranch(true)
    try {
      const coords = await storesApi.geocode(query)
      if (!coords) {
        setMessage("Couldn't find that address — double-check it or enter coordinates manually.")
        return
      }
      setDraft((d) => ({ ...d, latitude: coords.lat, longitude: coords.lng }))
    } catch {
      setMessage("Couldn't look up that address right now — try again or enter coordinates manually.")
    } finally {
      setGeocodingBranch(false)
    }
  }

  function startEditBranch(branch: OwnerBranch) {
    setMessage(null)
    setEditingBranchId(branch.id)
    setEditBranchDraft(toEditBranchDraft(branch))
  }

  function cancelEditBranch() {
    setEditingBranchId(null)
    setEditBranchDraft(null)
  }

  async function handleSaveBranch(event: FormEvent, branch: OwnerBranch) {
    event.preventDefault()
    if (!editBranchDraft) return
    setEditBranchSubmitting(true)
    setMessage(null)
    try {
      const payload: UpdateOwnerBranch = { ...editBranchDraft }
      const updated = await supermarketOwnerApi.updateBranch(branch.id, payload)
      setBranches((prev) => prev.map((b) => (b.id === updated.id ? updated : b)))
      cancelEditBranch()
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Could not update the branch.')
    } finally {
      setEditBranchSubmitting(false)
    }
  }

  async function handleDeleteBranch() {
    if (!deleteBranchTarget) return
    setDeletingBranch(true)
    setMessage(null)
    try {
      await supermarketOwnerApi.deleteBranch(deleteBranchTarget.id)
      setBranches((prev) => prev.filter((b) => b.id !== deleteBranchTarget.id))
      setProductBranchId((prev) => (prev === deleteBranchTarget.id ? null : prev))
      setDeleteBranchTarget(null)
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : 'Could not delete the branch. Try marking it inactive instead.',
      )
    } finally {
      setDeletingBranch(false)
    }
  }

  async function handleCreateProduct(event: FormEvent) {
    event.preventDefault()
    if (!productBranchId) return
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
        imageUrl: productDraft.imageUrl || undefined,
        branchId: productBranchId,
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
    setEditDraft(toEditProductDraft(product))
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

  async function handleDeleteProduct() {
    if (!deleteProductTarget) return
    setDeletingProduct(true)
    setMessage(null)
    try {
      await supermarketOwnerApi.deleteProduct(deleteProductTarget.id)
      setProducts((prev) => prev.filter((p) => p.id !== deleteProductTarget.id))
      setDeleteProductTarget(null)
    } catch (error) {
      setMessage(
        error instanceof Error ? error.message : 'Could not delete the product. Try marking it inactive instead.',
      )
    } finally {
      setDeletingProduct(false)
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

  const emailVerified = Boolean(user?.emailVerified)

  function branchFormFields(
    draft: NewOwnerBranch,
    setDraft: (updater: (d: NewOwnerBranch) => NewOwnerBranch) => void,
  ) {
    return (
      <>
        <input
          className="input-field"
          placeholder="Branch name (e.g. Downtown)"
          value={draft.name}
          onChange={(e) => setDraft((d) => ({ ...d, name: e.target.value }))}
          required
        />
        <input
          className="input-field"
          placeholder="Address line"
          value={draft.addressLine}
          onChange={(e) => setDraft((d) => ({ ...d, addressLine: e.target.value }))}
        />
        <input
          className="input-field"
          placeholder="City"
          value={draft.city}
          onChange={(e) => setDraft((d) => ({ ...d, city: e.target.value }))}
        />
        <button
          type="button"
          className="btn-secondary self-start px-3 py-1.5 text-xs"
          onClick={() => handleLocateAddress(draft, setDraft)}
          disabled={geocodingBranch}
        >
          {geocodingBranch ? 'Locating…' : 'Use address to find coordinates'}
        </button>
        <div className="flex gap-2">
          <input
            className="input-field"
            type="number"
            step="any"
            placeholder="Latitude"
            value={draft.latitude || ''}
            onChange={(e) => setDraft((d) => ({ ...d, latitude: Number(e.target.value) }))}
            required
          />
          <input
            className="input-field"
            type="number"
            step="any"
            placeholder="Longitude"
            value={draft.longitude || ''}
            onChange={(e) => setDraft((d) => ({ ...d, longitude: Number(e.target.value) }))}
            required
          />
        </div>
        <p className="text-xs text-ink-muted">
          Type the address and city above, then tap "Use address to find coordinates" — accurate coordinates are
          what let customers find your store nearby. Manually-typed coordinates are easy to get wrong.
        </p>
        <div className="flex gap-2">
          <input
            className="input-field"
            type="time"
            value={draft.openingTime}
            onChange={(e) => setDraft((d) => ({ ...d, openingTime: e.target.value }))}
          />
          <input
            className="input-field"
            type="time"
            value={draft.closingTime}
            onChange={(e) => setDraft((d) => ({ ...d, closingTime: e.target.value }))}
          />
        </div>
      </>
    )
  }

  return (
    <div className="page-wide flex flex-col gap-6 px-5 py-8">
      <div>
        <h1 className="text-xl font-extrabold text-ink">Manage your store</h1>
        <p className="mt-1 text-sm text-ink-muted">Set up your branches and manage the products customers will see.</p>
      </div>

      {message && (
        <p role="alert" className="text-sm text-danger-500">
          {message}
        </p>
      )}

      {insights && (
        <section><div className="mb-2"><h2 className="text-sm font-bold text-ink">Store performance</h2><p className="text-xs text-ink-muted">Last 30 days</p></div><div className="grid grid-cols-2 gap-3 md:grid-cols-4"><div className="card"><p className="text-xs text-ink-faint">Orders</p><p className="text-2xl font-black text-ink">{insights.ordersLast30Days}</p></div><div className="card"><p className="text-xs text-ink-faint">Revenue</p><p className="text-2xl font-black text-ink">₹{insights.revenueLast30Days.toFixed(0)}</p></div><div className="card"><p className="text-xs text-ink-faint">Average order</p><p className="text-2xl font-black text-ink">₹{insights.averageOrderValue.toFixed(0)}</p></div><div className="card"><p className="text-xs text-ink-faint">Low stock</p><p className="text-2xl font-black text-ink">{insights.lowStockItems}</p></div></div>{insights.topProducts.length > 0 && <div className="mt-3 card"><h3 className="text-xs font-bold text-ink">Best sellers</h3><div className="mt-2 flex flex-wrap gap-2">{insights.topProducts.map((product) => <span key={product.name} className="rounded-full bg-brand-50 px-3 py-1 text-xs font-semibold text-brand-800">{product.name} · {product.unitsSold}</span>)}</div></div>}</section>
      )}

      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-bold text-ink">Branches</h2>
          {emailVerified && !showBranchForm && (
            <button type="button" className="text-xs font-semibold text-brand-700" onClick={() => setShowBranchForm(true)}>
              + Add branch
            </button>
          )}
        </div>

        {branches.length === 0 && !emailVerified && (
          <div className="card flex flex-col gap-2 bg-warning-50 text-sm text-warning-500">
            <p>Verify your email before setting up your branch — this keeps every live store genuine.</p>
            <Link to="/verify-email" className="btn-primary self-start px-4 py-2 text-xs">
              Verify email
            </Link>
          </div>
        )}

        {branches.map((branch) =>
          editingBranchId === branch.id && editBranchDraft ? (
            <form key={branch.id} onSubmit={(e) => handleSaveBranch(e, branch)} className="card flex flex-col gap-2">
              {branchFormFields(editBranchDraft, (updater) =>
                setEditBranchDraft((d) => (d ? { ...d, ...updater(d) } : d)),
              )}
              <label className="flex items-center gap-2 text-xs text-ink-muted">
                <input
                  type="checkbox"
                  checked={editBranchDraft.active}
                  onChange={(e) => setEditBranchDraft((d) => (d ? { ...d, active: e.target.checked } : d))}
                />
                Active (visible to customers)
              </label>
              <div className="flex gap-2">
                <button type="submit" className="btn-primary flex-1" disabled={editBranchSubmitting}>
                  {editBranchSubmitting ? 'Saving…' : 'Save changes'}
                </button>
                <button type="button" className="btn-secondary flex-1" onClick={cancelEditBranch}>
                  Cancel
                </button>
              </div>
            </form>
          ) : (
            <div key={branch.id} className="card flex items-start justify-between gap-2">
              <div className="text-sm text-ink-muted">
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-ink">{branch.name}</span>
                  <span
                    className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                      branch.active ? 'bg-brand-50 text-brand-700' : 'bg-danger-50 text-danger-500'
                    }`}
                  >
                    {branch.active ? 'Active' : 'Inactive'}
                  </span>
                </div>
                {branch.addressLine}, {branch.city} · {branch.openingTime}–{branch.closingTime}
              </div>
              <div className="flex shrink-0 items-center gap-2">
                <button
                  type="button"
                  onClick={() => startEditBranch(branch)}
                  className="rounded-full bg-surface-muted px-3 py-1 text-xs font-semibold text-ink"
                >
                  Edit
                </button>
                <button
                  type="button"
                  onClick={() => setDeleteBranchTarget(branch)}
                  className="rounded-full bg-danger-50 px-3 py-1 text-xs font-semibold text-danger-500"
                >
                  Delete
                </button>
              </div>
            </div>
          ),
        )}

        {showBranchForm && (
          <form onSubmit={handleCreateBranch} className="card flex flex-col gap-2">
            {branches.length === 0 && (
              <p className="text-sm text-ink-muted">
                Add your branch's location before adding products — customers can't discover your store without one.
              </p>
            )}
            {branchFormFields(branchDraft, (updater) => setBranchDraft((d) => ({ ...d, ...updater(d) })))}
            <div className="flex gap-2">
              <button type="submit" className="btn-primary flex-1" disabled={branchSubmitting}>
                {branchSubmitting ? 'Creating…' : 'Create branch'}
              </button>
              {branches.length > 0 && (
                <button type="button" className="btn-secondary flex-1" onClick={() => setShowBranchForm(false)}>
                  Cancel
                </button>
              )}
            </div>
          </form>
        )}
      </section>

      {branches.length > 0 && supermarket && (
        <section className="card flex flex-col gap-3 print:border-0 print:shadow-none">
          <div className="print:hidden"><h2 className="text-sm font-bold text-ink">Promote your store</h2><p className="text-xs text-ink-muted">Print a QR card or share your storefront link with existing customers.</p></div>
          <div className="grid gap-3 sm:grid-cols-2">{branches.map((branch) => {
            const url = `${window.location.origin}/stores/${branch.id}`
            return <div key={branch.id} className="rounded-2xl border border-black/10 p-4 text-center"><p className="text-lg font-black text-ink">{supermarket.name}</p><p className="text-sm text-ink-muted">{branch.name}</p><img className="mx-auto my-3 h-36 w-36" alt={`QR code for ${branch.name}`} src={`https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(url)}`} /><p className="break-all text-[10px] text-ink-faint">{url}</p><div className="mt-3 flex gap-2 print:hidden"><button type="button" className="btn-secondary flex-1 py-2 text-xs" onClick={() => navigator.clipboard.writeText(url)}>Copy link</button><a className="btn-secondary flex-1 py-2 text-xs" href={`https://wa.me/?text=${encodeURIComponent(`Order from ${supermarket.name}: ${url}`)}`} target="_blank" rel="noreferrer">WhatsApp</a></div></div>
          })}</div>
          <button type="button" className="btn-primary print:hidden" onClick={() => window.print()}>Print store QR cards</button>
        </section>
      )}

      <CouponManager
        api={ownerCouponApi}
        title="Store coupons"
        description="Create codes that apply only to purchases from your supermarket."
      />

      {branches.length > 0 && (
        <section className="card flex flex-col gap-3">
          <div><h2 className="text-sm font-bold text-ink">Bulk catalogue import</h2><p className="text-xs text-ink-muted">Upload up to 500 products using CSV headers: name, description, sku, price, currency, category, imageUrl, stock.</p></div>
          <select className="input-field" value={bulkBranchId ?? ''} onChange={(event) => setBulkBranchId(Number(event.target.value))}>{branches.map((branch) => <option key={branch.id} value={branch.id}>{branch.name}</option>)}</select>
          <label className="btn-secondary cursor-pointer text-center"><input type="file" accept=".csv,text/csv" className="sr-only" disabled={bulkImporting} onChange={(event) => { const file = event.target.files?.[0]; if (file) void handleCsvImport(file); event.target.value = '' }} />{bulkImporting ? 'Importing…' : 'Choose CSV file'}</label>
        </section>
      )}

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
            <input
              className="input-field"
              type="url"
              placeholder="Product image URL (optional)"
              value={productDraft.imageUrl}
              onChange={(e) => setProductDraft((d) => ({ ...d, imageUrl: e.target.value }))}
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
            {branches.length > 1 && (
              <select
                className="input-field"
                value={productBranchId ?? ''}
                onChange={(e) => setProductBranchId(Number(e.target.value))}
              >
                {branches.map((branch) => (
                  <option key={branch.id} value={branch.id}>
                    {branch.name}
                  </option>
                ))}
              </select>
            )}
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
                  <button
                    type="button"
                    onClick={() => setDeleteProductTarget(product)}
                    className="rounded-full bg-danger-50 px-3 py-1 text-xs font-semibold text-danger-500"
                  >
                    Delete
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

      <Dialog
        open={deleteBranchTarget !== null}
        onClose={() => setDeleteBranchTarget(null)}
        title={`Delete ${deleteBranchTarget?.name ?? 'this branch'}?`}
        actions={
          <>
            <button
              type="button"
              className="btn-primary bg-danger-500 active:bg-danger-600"
              disabled={deletingBranch}
              onClick={handleDeleteBranch}
            >
              {deletingBranch ? 'Deleting…' : 'Delete'}
            </button>
            <button type="button" className="btn-ghost" onClick={() => setDeleteBranchTarget(null)}>
              Cancel
            </button>
          </>
        }
      >
        This can&apos;t be undone. If this branch has past orders, we'll ask you to mark it inactive instead.
      </Dialog>

      <Dialog
        open={deleteProductTarget !== null}
        onClose={() => setDeleteProductTarget(null)}
        title={`Delete ${deleteProductTarget?.name ?? 'this product'}?`}
        actions={
          <>
            <button
              type="button"
              className="btn-primary bg-danger-500 active:bg-danger-600"
              disabled={deletingProduct}
              onClick={handleDeleteProduct}
            >
              {deletingProduct ? 'Deleting…' : 'Delete'}
            </button>
            <button type="button" className="btn-ghost" onClick={() => setDeleteProductTarget(null)}>
              Cancel
            </button>
          </>
        }
      >
        This can&apos;t be undone. If this product has past orders, we'll ask you to mark it inactive instead.
      </Dialog>
    </div>
  )
}
