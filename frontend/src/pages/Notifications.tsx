import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { notificationsApi } from '../api/growth'
import type { NotificationsResponse } from '../api/growth'
import { BellIcon } from '../components/icons'

export default function Notifications() {
  const navigate = useNavigate()
  const [data, setData] = useState<NotificationsResponse | null>(null)

  useEffect(() => { notificationsApi.list().then(setData).catch(() => {}) }, [])

  async function open(id: number, actionUrl: string | null) {
    await notificationsApi.markRead(id).catch(() => {})
    setData((current) => current ? {
      unreadCount: Math.max(0, current.unreadCount - (current.notifications.find((n) => n.id === id)?.read ? 0 : 1)),
      notifications: current.notifications.map((n) => n.id === id ? { ...n, read: true } : n),
    } : current)
    if (actionUrl) navigate(actionUrl)
  }

  return (
    <div className="page-narrow flex flex-col gap-4 px-5 py-6">
      <div><h1 className="text-xl font-extrabold text-ink">Notifications</h1><p className="text-sm text-ink-muted">Order updates and rewards in one place.</p></div>
      {data?.notifications.length === 0 && <div className="card py-10 text-center"><BellIcon className="mx-auto h-9 w-9 text-ink-faint" /><p className="mt-2 font-semibold text-ink">You’re all caught up</p></div>}
      {data?.notifications.map((notification) => (
        <button key={notification.id} type="button" onClick={() => open(notification.id, notification.actionUrl)} className={`card text-left ${notification.read ? 'opacity-70' : 'border border-brand-100 bg-brand-50/40'}`}>
          <div className="flex items-start justify-between gap-3"><h2 className="text-sm font-bold text-ink">{notification.title}</h2>{!notification.read && <span className="mt-1 h-2 w-2 rounded-full bg-brand-600" />}</div>
          <p className="mt-1 text-sm text-ink-muted">{notification.message}</p>
          <p className="mt-2 text-xs text-ink-faint">{new Date(notification.createdAt).toLocaleString()}</p>
        </button>
      ))}
    </div>
  )
}
