import { useState, useEffect, useCallback } from 'react'
import axios from '../utils/axios'

/**
 * Hook for fetching and dismissing in-app notifications (bell icon).
 */
export function useNotifications() {
  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(false)

  const fetchNotifications = useCallback(async () => {
    setLoading(true)
    try {
      const res = await axios.get('/api/notifications')
      setNotifications(Array.isArray(res?.data) ? res.data : [])
    } catch (err) {
      console.error('Failed to fetch notifications:', err)
      setNotifications([])
    } finally {
      setLoading(false)
    }
  }, [])

  const dismissNotification = useCallback(async (type, referenceId) => {
    try {
      await axios.post('/api/notifications/dismiss', { type, referenceId })
      setNotifications(prev => prev.filter(
        n => !(n.type === type && n.referenceId === referenceId)
      ))
    } catch (err) {
      console.error('Failed to dismiss notification:', err)
    }
  }, [])

  useEffect(() => {
    fetchNotifications()
  }, [fetchNotifications])

  useEffect(() => {
    const onRefresh = () => fetchNotifications()
    window.addEventListener('mealcraft:notifications-refresh', onRefresh)
    return () => window.removeEventListener('mealcraft:notifications-refresh', onRefresh)
  }, [fetchNotifications])

  return {
    notifications,
    loading,
    refetch: fetchNotifications,
    dismiss: dismissNotification,
    count: notifications.length,
  }
}
