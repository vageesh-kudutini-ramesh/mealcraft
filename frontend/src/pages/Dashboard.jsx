import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import axios from '../utils/axios'
import { AlertTriangle, ShoppingBag, ChefHat, Calendar, ListChecks, Package, BookOpen, UtensilsCrossed } from 'lucide-react'

/**
 * Dashboard Page
 * 
 * Main dashboard showing expiring items, alerts, statistics, and quick actions.
 * 
 * @author MealCraft Team
 */
const Dashboard = () => {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchDashboardStats()
  }, [])

  const fetchDashboardStats = async () => {
    try {
      const response = await axios.get('/api/dashboard/stats')
      setStats(response.data)
    } catch (error) {
      console.error('Error fetching dashboard stats:', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return <div className="text-center py-12">Loading dashboard...</div>
  }

  if (!stats) {
    return <div className="text-center py-12">Error loading dashboard</div>
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>

      {/* Alerts Section */}
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <h2 className="text-xl font-semibold text-gray-800 mb-4">Alerts & Notifications</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Expiring Soon Alert */}
          {stats.expiringSoonCount > 0 ? (
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <div className="flex items-center mb-3">
              <AlertTriangle className="text-yellow-600 mr-3" size={24} />
              <div>
                <h3 className="font-semibold text-yellow-800 text-lg">
                  Items Expiring Soon
                </h3>
                <p className="text-sm text-yellow-700">
                  {stats.expiringSoonCount} {stats.expiringSoonCount === 1 ? 'item' : 'items'} need attention
                </p>
              </div>
            </div>
            <ul className="text-sm text-yellow-700 space-y-1">
              {stats.expiringSoonItems.slice(0, 3).map((item) => (
                <li key={item.id}>
                  {item.itemName} - {item.daysUntilExpiry} days left
                </li>
              ))}
            </ul>
            <Link
              to="/pantry?filter=expiring"
              className="text-yellow-700 hover:text-yellow-900 text-sm font-medium mt-2 inline-block"
            >
              View all →
            </Link>
          </div>
          ) : (
            <div className="bg-green-50 border border-green-200 rounded-lg p-4">
              <div className="flex items-center">
                <AlertTriangle className="text-green-600 mr-3" size={24} />
                <div>
                  <h3 className="font-semibold text-green-800 text-lg">All Good!</h3>
                  <p className="text-sm text-green-700">No items expiring soon</p>
                </div>
              </div>
            </div>
          )}

        {/* Expired Alert */}
        {stats.expiredCount > 0 ? (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-center mb-3">
              <AlertTriangle className="text-red-600 mr-3" size={24} />
              <div>
                <h3 className="font-semibold text-red-800 text-lg">
                  Expired Items
                </h3>
                <p className="text-sm text-red-700">
                  {stats.expiredCount} {stats.expiredCount === 1 ? 'item has' : 'items have'} expired
                </p>
              </div>
            </div>
            <ul className="text-sm text-red-700 space-y-1">
              {stats.expiredItems.slice(0, 3).map((item) => (
                <li key={item.id}>{item.itemName}</li>
              ))}
            </ul>
            <Link
              to="/pantry?filter=expired"
              className="text-red-700 hover:text-red-900 text-sm font-medium mt-2 inline-block"
            >
              View all →
            </Link>
          </div>
        ) : (
          <div className="bg-green-50 border border-green-200 rounded-lg p-4">
            <div className="flex items-center">
              <AlertTriangle className="text-green-600 mr-3" size={24} />
              <div>
                <h3 className="font-semibold text-green-800 text-lg">No Expired Items</h3>
                <p className="text-sm text-green-700">All items are fresh</p>
              </div>
            </div>
          </div>
        )}

        {/* Low Stock Alert */}
        {stats.lowStockCount > 0 ? (
          <div className="bg-orange-50 border border-orange-200 rounded-lg p-4">
            <div className="flex items-center mb-3">
              <AlertTriangle className="text-orange-600 mr-3" size={24} />
              <div>
                <h3 className="font-semibold text-orange-800 text-lg">
                  Low Stock Alert
                </h3>
                <p className="text-sm text-orange-700">
                  {stats.lowStockCount} {stats.lowStockCount === 1 ? 'item is' : 'items are'} running low
                </p>
              </div>
            </div>
            <Link
              to="/pantry?filter=low-stock"
              className="text-orange-700 hover:text-orange-900 text-sm font-medium mt-2 inline-block"
            >
              View all →
            </Link>
          </div>
        ) : (
          <div className="bg-green-50 border border-green-200 rounded-lg p-4">
            <div className="flex items-center">
              <AlertTriangle className="text-green-600 mr-3" size={24} />
              <div>
                <h3 className="font-semibold text-green-800 text-lg">Stock Levels Good</h3>
                <p className="text-sm text-green-700">All items are well stocked</p>
              </div>
            </div>
          </div>
        )}
        </div>
      </div>

      {/* Quick Actions */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold mb-4 text-gray-800">Quick Actions</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Link
            to="/recipes"
            className="flex flex-col items-center p-6 border-2 border-gray-200 rounded-lg hover:bg-primary-50 hover:border-primary-300 transition-all cursor-pointer group"
            title="Get recipe suggestions based on your pantry ingredients"
          >
            <ChefHat className="text-primary-600 mb-3" size={40} />
            <span className="text-base font-semibold text-gray-800">Suggest Recipes</span>
            <span className="text-xs text-gray-500 mt-1 text-center">Find recipes using your pantry items</span>
          </Link>
          <Link
            to="/pantry"
            className="flex flex-col items-center p-6 border-2 border-gray-200 rounded-lg hover:bg-primary-50 hover:border-primary-300 transition-all cursor-pointer"
            title="View and manage your pantry inventory"
          >
            <ShoppingBag className="text-primary-600 mb-3" size={40} />
            <span className="text-base font-semibold text-gray-800">View Pantry</span>
            <span className="text-xs text-gray-500 mt-1 text-center">Manage your pantry items</span>
          </Link>
          <Link
            to="/meal-plan"
            className="flex flex-col items-center p-6 border-2 border-gray-200 rounded-lg hover:bg-primary-50 hover:border-primary-300 transition-all cursor-pointer"
            title="Plan your weekly meals"
          >
            <Calendar className="text-primary-600 mb-3" size={40} />
            <span className="text-base font-semibold text-gray-800">Plan Meals</span>
            <span className="text-xs text-gray-500 mt-1 text-center">Schedule your weekly meals</span>
          </Link>
          <Link
            to="/shopping-list"
            className="flex flex-col items-center p-6 border-2 border-gray-200 rounded-lg hover:bg-primary-50 hover:border-primary-300 transition-all cursor-pointer"
            title="View your shopping list"
          >
            <ListChecks className="text-primary-600 mb-3" size={40} />
            <span className="text-base font-semibold text-gray-800">Shopping List</span>
            <span className="text-xs text-gray-500 mt-1 text-center">Items to buy</span>
          </Link>
        </div>
      </div>

      {/* Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center mb-3">
            <Package className="text-primary-600 mr-3" size={24} />
            <h3 className="text-lg font-semibold text-gray-800">Pantry Items</h3>
          </div>
          <p className="text-3xl font-bold text-primary-600">{stats.totalPantryItems}</p>
          <p className="text-sm text-gray-500 mt-1">Total items in your pantry</p>
        </div>
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center mb-3">
            <BookOpen className="text-primary-600 mr-3" size={24} />
            <h3 className="text-lg font-semibold text-gray-800">Saved Recipes</h3>
          </div>
          <p className="text-3xl font-bold text-primary-600">{stats.savedRecipesCount}</p>
          <p className="text-sm text-gray-500 mt-1">Recipes in your collection</p>
        </div>
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center mb-3">
            <UtensilsCrossed className="text-primary-600 mr-3" size={24} />
            <h3 className="text-lg font-semibold text-gray-800">Today's Meals</h3>
          </div>
          <p className="text-3xl font-bold text-primary-600">{stats.todaysMealPlan?.length || 0}</p>
          <p className="text-sm text-gray-500 mt-1">Meals planned for today</p>
        </div>
      </div>

      {/* Recent Recipes */}
      {stats.recentRecipes && stats.recentRecipes.length > 0 && (
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center mb-4">
            <BookOpen className="text-primary-600 mr-2" size={24} />
            <h2 className="text-xl font-semibold text-gray-800">Recently Saved Recipes</h2>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {stats.recentRecipes.map((recipe) => (
              <Link
                key={recipe.id}
                to={`/recipes/${recipe.id}`}
                className="border border-gray-200 rounded-lg p-4 hover:shadow-md hover:border-primary-300 transition-all"
              >
                {recipe.imageUrl && (
                  <img 
                    src={recipe.imageUrl} 
                    alt={recipe.recipeName} 
                    className="w-full h-32 object-cover rounded mb-3"
                    onError={(e) => {
                      e.target.style.display = 'none'
                    }}
                  />
                )}
                <h3 className="font-semibold text-gray-800 mb-2">{recipe.recipeName}</h3>
                <p className="text-sm text-gray-600">
                  {recipe.prepTimeMinutes || 0} min prep • {recipe.cookTimeMinutes || 0} min cook
                </p>
                {recipe.matchPercentage && (
                  <p className="text-xs text-primary-600 mt-1">
                    {recipe.matchPercentage.toFixed(0)}% match with your pantry
                  </p>
                )}
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

export default Dashboard



