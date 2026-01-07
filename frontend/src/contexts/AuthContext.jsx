import { createContext, useContext, useState, useEffect } from 'react'
import axios from '../utils/axios'

/**
 * Authentication Context
 * 
 * Manages user authentication state and provides authentication methods.
 * Stores JWT token in localStorage for session persistence.
 * 
 * @author MealCraft Team
 */
const AuthContext = createContext()

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(localStorage.getItem('token'))
  const [loading, setLoading] = useState(true)

  /**
   * Initialize auth state from localStorage
   */
  useEffect(() => {
    const storedToken = localStorage.getItem('token')
    const storedUser = localStorage.getItem('user')
    
    if (storedToken && storedUser) {
      setToken(storedToken)
      setUser(JSON.parse(storedUser))
      // Set default authorization header
      axios.defaults.headers.common['Authorization'] = `Bearer ${storedToken}`
    }
    setLoading(false)
  }, [])

  /**
   * Login user
   * 
   * @param {string} email - User email
   * @param {string} password - User password
   * @returns {Promise} - Login response
   */
  const login = async (email, password) => {
    try {
      const response = await axios.post('/api/auth/login', { email, password })
      const { token: newToken, userId, email: userEmail, fullName, initials } = response.data
      
      // Store token and user info
      localStorage.setItem('token', newToken)
      const userData = { id: userId, email: userEmail, fullName, initials }
      localStorage.setItem('user', JSON.stringify(userData))
      
      // Update state
      setToken(newToken)
      setUser(userData)
      
      // Set default authorization header
      axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`
      
      return response.data
    } catch (error) {
      throw error.response?.data || error.message
    }
  }

  /**
   * Register new user
   * 
   * @param {Object} userData - Registration data
   * @returns {Promise} - Registration response
   */
  const register = async (userData) => {
    try {
      const response = await axios.post('/api/auth/register', userData)
      const { token: newToken, userId, email: userEmail, fullName, initials } = response.data
      
      // Store token and user info
      localStorage.setItem('token', newToken)
      const userInfo = { id: userId, email: userEmail, fullName, initials }
      localStorage.setItem('user', JSON.stringify(userInfo))
      
      // Update state
      setToken(newToken)
      setUser(userInfo)
      
      // Set default authorization header
      axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`
      
      return response.data
    } catch (error) {
      throw error.response?.data || error.message
    }
  }

  /**
   * Logout user
   */
  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    delete axios.defaults.headers.common['Authorization']
    setToken(null)
    setUser(null)
  }

  /**
   * Update user profile
   * 
   * @param {Object} profileData - Updated profile data
   * @returns {Promise} - Updated user data
   */
  const updateProfile = async (profileData) => {
    try {
      const response = await axios.put('/api/users/profile', profileData)
      const { fullName, initials } = response.data
      
      // Update stored user info
      const updatedUser = { ...user, ...response.data }
      localStorage.setItem('user', JSON.stringify(updatedUser))
      setUser(updatedUser)
      
      return response.data
    } catch (error) {
      throw error.response?.data || error.message
    }
  }

  const value = {
    user,
    token,
    loading,
    login,
    register,
    logout,
    updateProfile,
    isAuthenticated: !!token,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}




