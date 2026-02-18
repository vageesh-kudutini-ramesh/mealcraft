import { createContext, useContext, useState } from 'react'

/**
 * Notification Context
 * 
 * Manages application-wide notifications and alerts.
 * Provides methods to show success, error, warning, and info notifications.
 * 
 * @author MealCraft Team
 */
const NotificationContext = createContext()

export const useNotification = () => {
  const context = useContext(NotificationContext)
  if (!context) {
    throw new Error('useNotification must be used within a NotificationProvider')
  }
  return context
}

export const NotificationProvider = ({ children }) => {
  const [notifications, setNotifications] = useState([])

  /**
   * Show a notification
   * 
   * @param {string} message - Notification message
   * @param {string} type - Notification type (success, error, warning, info)
   * @param {number} duration - Duration in milliseconds (default: 5000)
   */
  const showNotification = (message, type = 'info', duration = 5000) => {
    const id = Date.now()
    const notification = { id, message, type, duration }
    
    setNotifications((prev) => [...prev, notification])
    
    // Auto-remove after duration
    setTimeout(() => {
      removeNotification(id)
    }, duration)
  }

  /**
   * Remove a notification
   * 
   * @param {number} id - Notification ID
   */
  const removeNotification = (id) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id))
  }

  /**
   * Show success notification
   */
  const showSuccess = (message, duration) => {
    showNotification(message, 'success', duration)
  }

  /**
   * Show error notification
   */
  const showError = (message, duration) => {
    showNotification(message, 'error', duration)
  }

  /**
   * Show warning notification
   */
  const showWarning = (message, duration) => {
    showNotification(message, 'warning', duration)
  }

  /**
   * Show info notification
   */
  const showInfo = (message, duration) => {
    showNotification(message, 'info', duration)
  }

  const value = {
    notifications,
    showNotification,
    showSuccess,
    showError,
    showWarning,
    showInfo,
    removeNotification,
  }

  return (
    <NotificationContext.Provider value={value}>
      {children}
      {/* Notification container */}
      <div className="fixed top-4 right-4 z-50 space-y-2 font-body">
        {notifications.map((notification) => (
          <div
            key={notification.id}
            className={`px-5 py-4 rounded-2xl shadow-card min-w-[320px] max-w-md backdrop-blur-sm ${
              notification.type === 'success'
                ? 'bg-emerald-500 text-white'
                : notification.type === 'error'
                ? 'bg-red-500 text-white'
                : notification.type === 'warning'
                ? 'bg-amber-500 text-white'
                : 'bg-primary-500 text-white'
            }`}
          >
            <div className="flex items-center justify-between">
              <p>{notification.message}</p>
              <button
                onClick={() => removeNotification(notification.id)}
                className="ml-4 text-white hover:text-gray-200"
              >
                ×
              </button>
            </div>
          </div>
        ))}
      </div>
    </NotificationContext.Provider>
  )
}




