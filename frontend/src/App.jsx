import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { NotificationProvider } from './contexts/NotificationContext'
import ProtectedRoute from './components/auth/ProtectedRoute'
import Login from './pages/auth/Login'
import Register from './pages/auth/Register'
import Dashboard from './pages/Dashboard'
import Pantry from './pages/Pantry'
import Recipes from './pages/Recipes'
import MealPlan from './pages/MealPlan'
import ShoppingList from './pages/ShoppingList'
import Profile from './pages/Profile'
import Layout from './components/layout/Layout'

/**
 * Main App Component
 * 
 * Sets up routing, context providers, and protected routes.
 * 
 * @author MealCraft Team
 */
function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <Router>
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            
            {/* Protected Routes */}
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <Layout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<Dashboard />} />
              <Route path="pantry" element={<Pantry />} />
              <Route path="recipes" element={<Recipes />} />
              <Route path="meal-plan" element={<MealPlan />} />
              <Route path="shopping-list" element={<ShoppingList />} />
              <Route path="profile" element={<Profile />} />
            </Route>
            
            {/* Catch all - redirect to dashboard */}
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </Router>
      </NotificationProvider>
    </AuthProvider>
  )
}

export default App




