import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Bell,
  X,
  Clock,
  ShoppingCart,
  Calendar,
  BookOpen,
  Package,
  AlertCircle,
  TrendingUp,
  Sun,
  ChefHat,
} from 'lucide-react'
import { useNotifications } from '../../hooks/useNotifications'

const ICON_MAP = {
  clock: Clock,
  'shopping-cart': ShoppingCart,
  calendar: Calendar,
  'book-open': BookOpen,
  package: Package,
  'alert-circle': AlertCircle,
  'trending-up': TrendingUp,
  sun: Sun,
  'chef-hat': ChefHat,
}

const SEVERITY_STYLES = {
  info: 'bg-blue-50 border-blue-200 text-blue-800',
  warning: 'bg-amber-50 border-amber-200 text-amber-900',
  success: 'bg-emerald-50 border-emerald-200 text-emerald-800',
  tip: 'bg-violet-50 border-violet-200 text-violet-800',
}

const SEVERITY_ACCENT = {
  info: 'text-blue-500',
  warning: 'text-amber-500',
  success: 'text-emerald-500',
  tip: 'text-violet-500',
}

const NotificationBell = () => {
  const { notifications, loading, dismiss, refetch, count } = useNotifications()
  const [open, setOpen] = useState(false)

  useEffect(() => {
    if (!open) return
    refetch()
  }, [open, refetch])

  useEffect(() => {
    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') refetch()
    }
    document.addEventListener('visibilitychange', onVisibilityChange)
    return () => document.removeEventListener('visibilitychange', onVisibilityChange)
  }, [refetch])

  const IconComponent = (name) => ICON_MAP[name] || Bell
  const getSeverityStyle = (s) => SEVERITY_STYLES[s] || SEVERITY_STYLES.info
  const getAccent = (s) => SEVERITY_ACCENT[s] || SEVERITY_ACCENT.info

  return (
    <div className="relative">
      <button
        onClick={() => setOpen(!open)}
        className="p-2.5 rounded-xl text-slate-600 hover:bg-slate-100 hover:text-slate-900 transition-colors relative"
        aria-label={`Notifications ${count > 0 ? `(${count} unread)` : ''}`}
      >
        <Bell size={20} className="relative" />
        {count > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 flex items-center justify-center text-[10px] font-bold bg-accent-500 text-white rounded-full">
            {count > 99 ? '99+' : count}
          </span>
        )}
      </button>

      <AnimatePresence>
        {open && (
          <>
            <div
              className="fixed inset-0 z-40"
              onClick={() => setOpen(false)}
              aria-hidden="true"
            />
            <motion.div
              initial={{ opacity: 0, y: -8, scale: 0.96 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -8, scale: 0.96 }}
              transition={{ type: 'spring', duration: 0.25 }}
              className="absolute right-0 mt-2 w-[380px] max-w-[calc(100vw-2rem)] max-h-[85vh] overflow-hidden bg-white rounded-2xl shadow-xl border border-slate-200 z-50 flex flex-col"
            >
              <div className="px-4 py-3 border-b border-slate-100 bg-gradient-to-r from-primary-50/80 to-transparent">
                <div className="flex items-center justify-between">
                  <h3 className="font-semibold text-slate-900 flex items-center gap-2">
                    <Bell size={18} className="text-primary-500" />
                    Notifications
                  </h3>
                  {count > 0 && (
                    <span className="text-xs font-medium text-primary-600 bg-primary-100 px-2 py-0.5 rounded-full">
                      {count} new
                    </span>
                  )}
                </div>
              </div>

              <div className="overflow-y-auto overscroll-contain flex-1 py-2">
                {loading ? (
                  <div className="flex items-center justify-center py-12">
                    <div className="animate-spin rounded-full h-8 w-8 border-2 border-primary-500 border-t-transparent" />
                  </div>
                ) : notifications.length === 0 ? (
                  <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
                    <div className="w-14 h-14 rounded-2xl bg-slate-100 flex items-center justify-center mb-3">
                      <Bell size={24} className="text-slate-400" />
                    </div>
                    <p className="text-sm font-medium text-slate-600">All caught up!</p>
                    <p className="text-xs text-slate-500 mt-1">No new notifications</p>
                  </div>
                ) : (
                  <div className="space-y-1 px-2">
                    {notifications.map((n) => {
                      const Icon = IconComponent(n.icon)
                      const severity = getSeverityStyle(n.severity)
                      const accent = getAccent(n.severity)
                      const content = (
                        <div
                          className={`group flex gap-3 p-3 rounded-xl border transition-colors ${severity} hover:shadow-sm`}
                        >
                          <div className={`flex-shrink-0 w-10 h-10 rounded-xl flex items-center justify-center ${severity} ${accent}`}>
                            <Icon size={20} strokeWidth={2} />
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="font-semibold text-sm">{n.title}</p>
                            <p className="text-sm opacity-90 mt-0.5">{n.message}</p>
                            {n.subtitle && (
                              <p className="text-xs opacity-80 mt-1">{n.subtitle}</p>
                            )}
                            {n.recipeMatches && n.recipeMatches.length > 0 && (
                              <div className="mt-2 flex flex-wrap gap-2">
                                {n.recipeMatches.slice(0, 3).map((r) => (
                                  <span
                                    key={r.externalRecipeId || r.id || r.recipeName}
                                    className="text-xs px-2 py-1 rounded-lg bg-white/60"
                                  >
                                    {r.recipeName}
                                  </span>
                                ))}
                              </div>
                            )}
                          </div>
                          <button
                            onClick={(e) => {
                              e.stopPropagation()
                              e.preventDefault()
                              dismiss(n.type, n.referenceId)
                            }}
                            className="flex-shrink-0 p-1.5 rounded-lg opacity-60 hover:opacity-100 hover:bg-black/5 transition-colors"
                            aria-label="Dismiss"
                          >
                            <X size={16} />
                          </button>
                        </div>
                      )
                      return n.actionUrl ? (
                        <Link
                          key={n.id}
                          to={n.actionUrl}
                          onClick={() => setOpen(false)}
                          className="block"
                        >
                          {content}
                        </Link>
                      ) : (
                        <div key={n.id}>{content}</div>
                      )
                    })}
                  </div>
                )}
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </div>
  )
}

export default NotificationBell
