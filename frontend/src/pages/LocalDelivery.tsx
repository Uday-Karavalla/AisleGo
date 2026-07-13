import { Link, useParams } from 'react-router-dom'
import { Seo } from '../components/Seo'
import { ClockIcon, ShieldCheckIcon, StoreIcon, TruckIcon } from '../components/icons'

function titleCase(value: string) {
  return value.replace(/-/g, ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())
}

export default function LocalDelivery() {
  const { city = 'near-you' } = useParams<{ city: string }>()
  const cityName = titleCase(city)
  const description = `Order groceries online from verified supermarkets in ${cityName}. Compare nearby stores, use local offers, and choose delivery, scheduled delivery or pickup.`
  return (
    <div className="page-wide flex flex-col gap-8 px-5 py-8">
      <Seo title={`Online grocery delivery in ${cityName} | AisleGo`} description={description} canonicalPath={`/delivery/${city}`} structuredData={{ '@context': 'https://schema.org', '@type': 'Service', name: `AisleGo grocery delivery in ${cityName}`, areaServed: cityName, serviceType: 'Grocery delivery', provider: { '@type': 'Organization', name: 'AisleGo' } }} />
      <section className="rounded-3xl bg-gradient-to-br from-brand-800 to-brand-600 px-6 py-12 text-center text-white"><p className="text-xs font-bold uppercase tracking-[0.2em] text-brand-200">Local grocery marketplace</p><h1 className="mt-3 text-3xl font-black sm:text-5xl">Grocery delivery in {cityName}</h1><p className="mx-auto mt-4 max-w-2xl text-white/80">{description}</p><Link to="/" className="mt-6 inline-flex rounded-xl bg-white px-5 py-3 font-bold text-brand-800">Find stores near me</Link></section>
      <div className="grid gap-4 sm:grid-cols-3">{[[StoreIcon, 'Real local stores', 'Shop verified supermarket catalogues instead of an anonymous warehouse.'], [ClockIcon, 'Flexible fulfilment', 'Choose ASAP delivery, schedule a convenient time, or collect for free.'], [ShieldCheckIcon, 'Trusted checkout', 'See complete pricing, verified reviews and order updates before and after payment.']].map(([Icon, title, text]) => { const C = Icon as typeof TruckIcon; return <article key={title as string} className="card"><C className="h-7 w-7 text-brand-600" /><h2 className="mt-3 font-bold text-ink">{title as string}</h2><p className="mt-1 text-sm text-ink-muted">{text as string}</p></article> })}</div>
    </div>
  )
}
