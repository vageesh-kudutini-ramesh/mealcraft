import axios from 'axios'

/**
 * Axios instance configuration
 * 
 * Configures base URL and default headers for API requests.
 * 
 * @author MealCraft Team
 */
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor - add auth token if available
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor - handle errors
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      console.error('Authentication error (401):', error.response?.data)
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      delete axiosInstance.defaults.headers.common['Authorization']
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    } else if (error.response?.status === 403) {
      // 403 Forbidden - could be auth issue or permission issue
      // Check error message to determine if it's auth-related
      const errorMessage = error.response?.data?.message || error.response?.data?.error || ''
      const errorStr = JSON.stringify(error.response?.data || {}).toLowerCase()
      const isAuthError = errorMessage.toLowerCase().includes('unauthorized') || 
                         errorMessage.toLowerCase().includes('authentication') ||
                         errorMessage.toLowerCase().includes('token') ||
                         errorMessage.toLowerCase().includes('login') ||
                         errorMessage.toLowerCase().includes('authentication required') ||
                         errorStr.includes('authentication required') ||
                         errorStr.includes('invalid authentication')
      
      if (isAuthError) {
        console.error('Authentication error (403):', error.response?.data)
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        delete axiosInstance.defaults.headers.common['Authorization']
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      } else {
        // Permission error, not auth error - don't log out
        console.warn('Permission/access error (403):', error.response?.data)
        // Don't log out, just let the component handle the error
      }
    }
    return Promise.reject(error)
  }
)

export default axiosInstance




