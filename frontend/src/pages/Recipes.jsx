import { useState } from 'react'
import axios from '../utils/axios'
import { Search, ChefHat } from 'lucide-react'
import { useNotification } from '../contexts/NotificationContext'

/**
 * Recipes Page
 * 
 * Recipe suggestions and saved recipes management.
 * 
 * @author MealCraft Team
 */
const Recipes = () => {
  const [suggestions, setSuggestions] = useState([])
  const [savedRecipes, setSavedRecipes] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const { showSuccess, showError } = useNotification()

  const handleSuggestRecipes = async () => {
    setLoading(true)
    try {
      const response = await axios.post('/api/recipes/suggest', { mealType: 'ALL' })
      setSuggestions(response.data)
    } catch (error) {
      showError('Error fetching recipe suggestions')
    } finally {
      setLoading(false)
    }
  }

  const handleSaveRecipe = async (recipe) => {
    try {
      await axios.post('/api/recipes/saved', recipe)
      showSuccess('Recipe saved successfully!')
    } catch (error) {
      showError('Error saving recipe')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Recipes</h1>
        <button
          onClick={handleSuggestRecipes}
          disabled={loading}
          className="bg-primary-600 text-white px-4 py-2 rounded-lg hover:bg-primary-700 disabled:opacity-50 flex items-center"
        >
          <ChefHat size={20} className="mr-2" />
          {loading ? 'Loading...' : 'Suggest Recipes'}
        </button>
      </div>

      {/* Search Saved Recipes */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
        <input
          type="text"
          placeholder="Search saved recipes..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 text-gray-900 placeholder:text-gray-400"
        />
      </div>

      {/* Recipe Suggestions */}
      {suggestions.length > 0 && (
        <div>
          <h2 className="text-xl font-semibold mb-4">Suggested Recipes</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {suggestions.map((recipe) => (
              <div key={recipe.externalRecipeId || recipe.id} className="bg-white rounded-lg shadow p-4">
                {recipe.imageUrl && (
                  <img src={recipe.imageUrl} alt={recipe.recipeName} className="w-full h-48 object-cover rounded mb-4" />
                )}
                <h3 className="font-semibold text-lg mb-2">{recipe.recipeName}</h3>
                <p className="text-sm text-gray-600 mb-2">
                  Match: {recipe.matchPercentage?.toFixed(0)}% • {recipe.prepTimeMinutes} min prep • {recipe.cookTimeMinutes} min cook
                </p>
                {recipe.usesExpiringIngredients && (
                  <p className="text-sm text-yellow-600 mb-2">⚠️ Uses expiring ingredients</p>
                )}
                <button
                  onClick={() => handleSaveRecipe(recipe)}
                  className="w-full bg-primary-600 text-white py-2 rounded hover:bg-primary-700"
                >
                  Save Recipe
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Saved Recipes */}
      <div>
        <h2 className="text-xl font-semibold mb-4">Saved Recipes</h2>
        {savedRecipes.length === 0 ? (
          <p className="text-gray-500 text-center py-12">No saved recipes yet. Suggest recipes to get started!</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {savedRecipes.map((recipe) => (
              <div key={recipe.id} className="bg-white rounded-lg shadow p-4">
                <h3 className="font-semibold text-lg mb-2">{recipe.recipeName}</h3>
                <p className="text-sm text-gray-600">{recipe.prepTimeMinutes} min prep • {recipe.cookTimeMinutes} min cook</p>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default Recipes



