/**
 * Dispatches a custom event to refresh the notification bell.
 * Call after user actions that affect notifications (add pantry, plan meal, etc.)
 */
export function refreshNotifications() {
  window.dispatchEvent(new Event('mealcraft:notifications-refresh'))
}
