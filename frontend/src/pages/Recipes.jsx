import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import axios from '../utils/axios'
import { Search, ChefHat, X, ShoppingCart, Save, BookOpen, Clock, Users, AlertCircle, Compass, Sparkles, PartyPopper, RotateCcw, ChevronDown } from 'lucide-react'
import { useNotification } from '../contexts/NotificationContext'
import { refreshNotifications } from '../utils/notifications'
import SlidingTicker from '../components/common/SlidingTicker'
import YouTubeRecipeLink from '../components/recipes/YouTubeRecipeLink'
import RecipeImage from '../components/recipes/RecipeImage'
import { useAuth } from '../contexts/AuthContext'
import { OCCASION_TEMPLATES, OCCASION_LABELS, OCCASION_DISPLAY_COUNT, shuffleArray } from '../data/occasionTemplates'
import { getLocalDateStr } from '../utils/date'

/**
 * Recipes Page
 * 
 * Recipe suggestions and saved recipes management with enhanced pantry integration.
 * 
 * @author MealCraft Team
 */
const Recipes = () => {
  const [suggestions, setSuggestions] = useState([])
  const [savedRecipes, setSavedRecipes] = useState([])
  const [loading, setLoading] = useState(false)
  const [suggestionsAttempted, setSuggestionsAttempted] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [selectedRecipe, setSelectedRecipe] = useState(null)
  const [loadingDetails, setLoadingDetails] = useState(false)
  const [adjustedIngredients, setAdjustedIngredients] = useState({})
  const [areas, setAreas] = useState([])
  const [selectedArea, setSelectedArea] = useState('')
  const [selectedDiet, setSelectedDiet] = useState('ALL')
  const [exploreResults, setExploreResults] = useState([])
  const [exploreLoading, setExploreLoading] = useState(false)
  const [activeExplore, setActiveExplore] = useState(null)
  const [showOccasions, setShowOccasions] = useState(false)
  const [selectedOccasion, setSelectedOccasion] = useState(null)
  const [occasionItems, setOccasionItems] = useState([])
  const [suggestOffset, setSuggestOffset] = useState(0)
  const [exploreOffset, setExploreOffset] = useState(0)
  const [addToShopConfirmItem, setAddToShopConfirmItem] = useState(null)
  const [addToShopAdding, setAddToShopAdding] = useState(false)
  const { showSuccess, showError } = useNotification()

  const POPULAR_CUISINES = ['Italian', 'Indian', 'Mexican', 'Chinese', 'Japanese', 'Mediterranean']
  const { token } = useAuth()

  useEffect(() => {
    fetchSavedRecipes()
  }, [])

  useEffect(() => {
    const fetchAreas = async () => {
      try {
        const response = await axios.get('/api/recipes/areas')
        setAreas(response.data || [])
      } catch (err) {
        console.error('Error fetching cuisines:', err)
      }
    }
    if (token) fetchAreas()
  }, [token])

  // Refetch saved recipes when user returns to this tab/page
  useEffect(() => {
    const onVisibilityChange = () => {
      if (document.visibilityState === 'visible') fetchSavedRecipes()
    }
    document.addEventListener('visibilitychange', onVisibilityChange)
    return () => document.removeEventListener('visibilitychange', onVisibilityChange)
  }, [])

  const fetchSavedRecipes = async () => {
    try {
      const response = await axios.get('/api/recipes/saved')
      setSavedRecipes(Array.isArray(response?.data) ? response.data : [])
    } catch (error) {
      console.error('Error fetching saved recipes:', error)
      setSavedRecipes([])
    }
  }

  const handleSuggestRecipes = async (isRefresh = false) => {
    setLoading(true)
    setSuggestionsAttempted(true)
    setActiveExplore(null)
    setExploreResults([])
    const offset = isRefresh ? suggestOffset : 0
    try {
      const response = await axios.post('/api/recipes/suggest', {
        mealType: 'ALL',
        area: selectedArea || null,
        diet: selectedDiet || 'ALL',
        offset
      })
      setSuggestions(response.data || [])
      if (isRefresh) setSuggestOffset(prev => prev + 15)
      else setSuggestOffset(15)
    } catch (error) {
      showError('Error fetching recipe suggestions')
      setSuggestions([])
    } finally {
      setLoading(false)
    }
  }

  const handleDiscover = async (cuisine, diet, label, isRefresh = false) => {
    setExploreLoading(true)
    setActiveExplore(label || cuisine || diet || 'Explore')
    setSuggestions([])
    setSuggestionsAttempted(false)
    const offset = isRefresh ? exploreOffset : 0
    try {
      const params = new URLSearchParams()
      if (cuisine) params.set('cuisine', cuisine)
      if (diet && diet !== 'ALL') params.set('diet', diet)
      params.set('offset', offset)
      const res = await axios.get(`/api/recipes/discover?${params.toString()}`)
      setExploreResults(res.data || [])
      if (isRefresh) setExploreOffset(prev => prev + 15)
      else setExploreOffset(15)
    } catch (e) {
      showError('Could not load recipes')
      setExploreResults([])
    } finally {
      setExploreLoading(false)
    }
  }

  const handleRecipeClick = async (recipe) => {
    setSelectedRecipe(null)
    setIsModalOpen(true)
    setLoadingDetails(true)
    setAdjustedIngredients({})
    
    try {
      const recipeId = recipe.id || recipe.externalRecipeId
      if (!recipeId) {
        showError('Recipe ID is missing')
        setIsModalOpen(false)
        return
      }
      console.log('Fetching recipe details for ID:', recipeId)
      console.log('Token exists:', !!token)
      
      if (!token) {
        showError('Please log in to view recipe details')
        setIsModalOpen(false)
        return
      }
      
      const response = await axios.get(`/api/recipes/enhance/${recipeId}`)
      setSelectedRecipe(response.data)
    } catch (error) {
      console.error('Error loading recipe details:', error)
      console.error('Error response:', error.response?.data)
      console.error('Error status:', error.response?.status)
      
      // Check if it's an auth error
      const errorMessage = error.response?.data?.message || error.response?.data?.error || ''
      const isAuthError = error.response?.status === 401 || 
                         (error.response?.status === 403 && 
                          (errorMessage.toLowerCase().includes('authentication') ||
                           errorMessage.toLowerCase().includes('unauthorized') ||
                           errorMessage.toLowerCase().includes('token') ||
                           errorMessage.toLowerCase().includes('login')))
      
      if (isAuthError) {
        // Auth error - let the interceptor handle logout/redirect
        console.warn('Authentication error detected, interceptor will handle logout')
        setIsModalOpen(false)
        return
      }
      
      // For other errors, show error message
      const displayMessage = error.response?.data?.message || 
                            error.response?.data?.error || 
                            error.message || 
                            'Error loading recipe details. Please try again.'
      showError(displayMessage)
      setIsModalOpen(false)
    } finally {
      setLoadingDetails(false)
    }
  }

  const handleCloseModal = () => {
    setIsModalOpen(false)
    setSelectedRecipe(null)
    setAdjustedIngredients({})
  }

  const handleSaveRecipe = async (recipe) => {
    try {
      await axios.post('/api/recipes/saved', recipe)
      showSuccess('Recipe saved successfully!')
      fetchSavedRecipes()
    } catch (error) {
      showError('Error saving recipe')
    }
  }

  const handleUnsaveRecipe = async (recipeId) => {
    try {
      await axios.delete(`/api/recipes/saved/${recipeId}`)
      showSuccess('Recipe removed from saved recipes')
      fetchSavedRecipes()
      if (selectedRecipe?.id === recipeId) {
        handleCloseModal()
      }
    } catch (error) {
      showError('Error removing recipe')
    }
  }

  const handleQuantityChange = (ingredientName, value) => {
    setAdjustedIngredients(prev => ({
      ...prev,
      [ingredientName]: parseFloat(value) || 0
    }))
  }

  const handleCookRecipe = async () => {
    if (!selectedRecipe) return
    
    try {
      const recipeId = selectedRecipe.id || selectedRecipe.externalRecipeId
      await axios.post('/api/recipes/cook', {
        recipeId,
        adjustedIngredients
      })
      showSuccess('Recipe cooked! Pantry items updated.')
      refreshNotifications()
      handleCloseModal()
      // Refresh suggestions to update match percentages
      if (suggestions.length > 0) {
        handleSuggestRecipes()
      }
    } catch (error) {
      showError('Error cooking recipe')
    }
  }

  const handleAddToShoppingListClick = (ingredient) => {
    setAddToShopConfirmItem(ingredient)
  }

  const handleAddToShoppingListConfirm = async () => {
    const ingredient = addToShopConfirmItem
    if (!ingredient) return
    setAddToShopAdding(true)
    try {
      const rawQty = ingredient.requiredQuantity ?? ingredient.quantity ?? 1
      const qty = parseFloat(rawQty)
      const quantity = (typeof qty === 'number' && !isNaN(qty) && qty > 0) ? qty : 1
      const rawUnit = ingredient.requiredUnit || ingredient.unit || 'pieces'
      const unit = (rawUnit && String(rawUnit).trim()) ? String(rawUnit).trim() : 'pieces'

      await axios.post('/api/shopping-list', {
        itemName: String(ingredient.name || '').trim() || 'Ingredient',
        quantity,
        unit,
        isPurchased: false,
        addedAt: getLocalDateStr()
      })
      showSuccess(`${ingredient.name} added to shopping list!`)
      refreshNotifications()
      setAddToShopConfirmItem(null)
    } catch (error) {
      const msg = error.response?.data?.message || error.response?.data?.error || 'Error adding item to shopping list'
      showError(msg)
    } finally {
      setAddToShopAdding(false)
    }
  }

  const handleAddToShoppingListCancel = () => {
    setAddToShopConfirmItem(null)
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'Available':
        return 'bg-green-100 text-green-800'
      case 'Low Stock':
        return 'bg-yellow-100 text-yellow-800'
      case 'Insufficient':
        return 'bg-orange-100 text-orange-800'
      case 'Not in Pantry':
        return 'bg-red-100 text-red-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const filteredSavedRecipes = savedRecipes.filter(recipe =>
    recipe.recipeName?.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const isRecipeSaved = (recipe) => {
    if (!recipe.externalRecipeId) return false
    return savedRecipes.some(saved => saved.externalRecipeId === recipe.externalRecipeId)
  }

  const loadOccasionItems = useCallback(() => {
    if (selectedOccasion && OCCASION_TEMPLATES[selectedOccasion]) {
      const items = OCCASION_TEMPLATES[selectedOccasion].items
      setOccasionItems(shuffleArray(items).slice(0, OCCASION_DISPLAY_COUNT))
    } else {
      setOccasionItems([])
    }
  }, [selectedOccasion])

  useEffect(() => {
    loadOccasionItems()
  }, [loadOccasionItems])

  const handleSaveOccasionItem = async (itemName) => {
    const recipe = {
      recipeName: itemName,
      mealType: 'ALL',
      imageUrl: null,
      prepTimeMinutes: null,
      cookTimeMinutes: null,
      servings: null,
      instructions: null,
      ingredients: [],
    }
    try {
      await axios.post('/api/recipes/saved', recipe)
      showSuccess(`"${itemName}" saved to recipes!`)
      fetchSavedRecipes()
    } catch (e) {
      showError('Error saving recipe')
    }
  }

  const displayRecipes = suggestions.length > 0 ? suggestions : exploreResults
  const displaySource = suggestions.length > 0 ? 'suggested' : 'explore'
  const isLoading = loading || exploreLoading

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      {/* Hero + Quick actions */}
      <div className="bg-gradient-to-br from-primary-50 to-blue-50/30 rounded-2xl p-6 border border-primary-100">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
          <div>
            <h1 className="text-2xl md:text-3xl font-bold text-gray-900 flex items-center gap-2">
              <ChefHat size={32} className="text-primary-600" />
              Recipes
            </h1>
            <p className="text-gray-600 mt-1">Discover ideas from your pantry or explore cuisines. Save favorites and plan your week.</p>
          </div>
          <div className="flex flex-col sm:flex-row gap-3">
            <div className="flex gap-2 flex-wrap">
              <select
                value={selectedArea}
                onChange={(e) => setSelectedArea(e.target.value)}
                className="border border-gray-300 rounded-lg px-3 py-2 text-gray-900 bg-white focus:ring-2 focus:ring-primary-500 text-sm"
                aria-label="Cuisine"
              >
                <option value="">All cuisines</option>
                {areas.map((a) => (
                  <option key={a} value={a}>{a}</option>
                ))}
              </select>
              <select
                value={selectedDiet}
                onChange={(e) => setSelectedDiet(e.target.value)}
                className="border border-gray-300 rounded-lg px-3 py-2 text-gray-900 bg-white focus:ring-2 focus:ring-primary-500 text-sm"
                aria-label="Diet"
              >
                <option value="ALL">All</option>
                <option value="VEGETARIAN">Vegetarian</option>
                <option value="NON_VEGETARIAN">Non-Veg</option>
              </select>
            </div>
            <button
              onClick={handleSuggestRecipes}
              disabled={loading}
              className="bg-primary-600 text-white px-4 py-2.5 rounded-lg hover:bg-primary-700 disabled:opacity-50 flex items-center justify-center gap-2 font-medium shrink-0"
            >
              <Sparkles size={18} />
              {loading ? 'Loading...' : 'Suggest from pantry'}
            </button>
          </div>
        </div>
      </div>

      <SlidingTicker variant="compact" />

      {/* Occasion Templates – Party, Trekking, Trip, etc. */}
      <div className="rounded-2xl border-2 border-primary-100 overflow-hidden bg-gradient-to-br from-accent-50/50 via-white to-primary-50/50">
        <button
          type="button"
          onClick={() => setShowOccasions(!showOccasions)}
          className="w-full px-6 py-4 flex items-center justify-between gap-4 hover:bg-primary-50/50 transition-colors"
        >
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-accent-400 to-primary-500 flex items-center justify-center shadow-md">
              <PartyPopper className="w-6 h-6 text-white" />
            </div>
            <div className="text-left">
              <h2 className="text-lg font-bold text-gray-900">Plan for an occasion</h2>
              <p className="text-sm text-gray-600">Party, trekking, picnic, movie night & more – curated items with videos</p>
            </div>
          </div>
          <ChevronDown className={`w-6 h-6 text-primary-600 transition-transform ${showOccasions ? 'rotate-180' : ''}`} />
        </button>
        {showOccasions && (
          <div className="p-6 pt-0 space-y-5 border-t border-primary-100">
            <div>
              <p className="text-sm font-medium text-gray-700 mb-3">Choose an occasion:</p>
              <div className="flex flex-wrap gap-2">
                {OCCASION_LABELS.map((label) => {
                  const template = OCCASION_TEMPLATES[label]
                  const isSelected = selectedOccasion === label
                  return (
                    <button
                      key={label}
                      onClick={() => setSelectedOccasion(isSelected ? null : label)}
                      className={`inline-flex items-center gap-2 px-4 py-2.5 rounded-xl font-medium transition-all ${
                        isSelected
                          ? 'bg-primary-600 text-white shadow-md'
                          : 'bg-white border-2 border-primary-200 text-gray-700 hover:border-primary-400 hover:bg-primary-50'
                      }`}
                    >
                      <span className="text-lg">{template.icon}</span>
                      {label}
                    </button>
                  )
                })}
              </div>
            </div>
            {selectedOccasion && (
              <>
                <div className="flex items-center justify-between gap-4">
                  <p className="text-sm text-gray-600">
                    Click Watch to see how to make it on YouTube. Save to add to your recipes.
                  </p>
                  <button
                    onClick={loadOccasionItems}
                    className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-primary-100 text-primary-700 font-medium hover:bg-primary-200 transition-colors"
                  >
                    <RotateCcw size={18} />
                    Refresh – new items
                  </button>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                  {occasionItems.map((itemName) => (
                    <div
                      key={itemName}
                      className="bg-white rounded-xl shadow-sm border border-gray-200 p-4 hover:shadow-md transition-all flex flex-col"
                    >
                      <h4 className="font-semibold text-gray-900 mb-2">{itemName}</h4>
                      <div className="flex flex-wrap gap-2 mt-auto">
                        <YouTubeRecipeLink recipeName={itemName} size="sm" />
                        <button
                          onClick={(e) => {
                            e.stopPropagation()
                            handleSaveOccasionItem(itemName)
                          }}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary-600 text-white text-xs font-medium hover:bg-primary-700"
                        >
                          <Save size={14} />
                          Save
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        )}
      </div>

      {/* Explore – always visible */}
      <div>
        <h2 className="text-lg font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <Compass size={20} className="text-primary-600" />
          Explore by cuisine
        </h2>
        <p className="text-sm text-gray-500 mb-4">Browse recipes without adding pantry items. Click to discover.</p>
        <div className="flex flex-wrap gap-2">
          {POPULAR_CUISINES.map((c) => (
            <button
              key={c}
              onClick={() => handleDiscover(c, 'ALL', c)}
              disabled={exploreLoading}
              className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                activeExplore === c
                  ? 'bg-primary-600 text-white shadow-md'
                  : 'bg-white border border-gray-200 text-gray-700 hover:border-primary-300 hover:bg-primary-50'
              }`}
            >
              {c}
            </button>
          ))}
          <button
            onClick={() => handleDiscover(null, 'VEGETARIAN', 'Vegetarian')}
            disabled={exploreLoading}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
              activeExplore === 'Vegetarian'
                ? 'bg-emerald-600 text-white shadow-md'
                : 'bg-white border border-gray-200 text-gray-700 hover:border-emerald-300 hover:bg-emerald-50'
            }`}
          >
            Vegetarian
          </button>
        </div>
      </div>

      {/* Search Saved Recipes */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
        <input
          type="text"
          placeholder="Search saved recipes..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 text-gray-900 placeholder:text-gray-400"
        />
      </div>

      {/* Pantry tip when no suggestions */}
      {suggestionsAttempted && suggestions.length === 0 && !loading && !displayRecipes.length && (
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-5 text-amber-800">
          <p className="font-medium">No pantry-based suggestions yet.</p>
          <p className="text-sm mt-1">Add items to your <Link to="/pantry" className="underline font-medium">Pantry</Link>, or use the cuisine buttons above to explore recipes.</p>
        </div>
      )}

      {/* Recipe grid – suggested or explored */}
      {displayRecipes.length > 0 && (
        <div>
          <div className="flex flex-wrap items-center justify-between gap-4 mb-4">
            <h2 className="text-xl font-semibold text-gray-900">
              {displaySource === 'suggested' ? 'Suggested from your pantry' : `Exploring: ${activeExplore || 'Recipes'}`}
            </h2>
            <button
              onClick={() => {
                if (displaySource === 'suggested') {
                  handleSuggestRecipes(true)
                } else {
                  if (activeExplore === 'Vegetarian') {
                    handleDiscover(null, 'VEGETARIAN', 'Vegetarian', true)
                  } else {
                    handleDiscover(activeExplore, 'ALL', activeExplore, true)
                  }
                }
              }}
              disabled={isLoading}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-primary-100 text-primary-700 font-medium hover:bg-primary-200 transition-colors disabled:opacity-50"
            >
              <RotateCcw size={18} />
              Refresh – new dishes
            </button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {displayRecipes.map((recipe) => (
              <div
                key={recipe.externalRecipeId || recipe.id}
                onClick={() => handleRecipeClick(recipe)}
                className="bg-white rounded-lg shadow hover:shadow-lg transition-shadow cursor-pointer overflow-hidden"
              >
                <RecipeImage
                  src={recipe.imageUrl}
                  alt={recipe.recipeName}
                  className="w-full h-48 object-cover"
                />
                <div className="p-4">
                  <h3 className="font-semibold text-lg mb-1.5 text-gray-900">{recipe.recipeName}</h3>
                  <div className="mb-2">
                    <YouTubeRecipeLink recipeName={recipe.recipeName} size="sm" />
                  </div>
                  <div className="flex flex-wrap gap-2 text-sm text-gray-600 mb-3">
                    <span className="flex items-center gap-1">
                      <Users size={14} />
                      {recipe.matchPercentage != null ? `${Math.round(recipe.matchPercentage)}% match` : 'Discover'}
                    </span>
                    {recipe.prepTimeMinutes && (
                      <span className="flex items-center gap-1">
                        <Clock size={14} />
                        {recipe.prepTimeMinutes} min
                      </span>
                    )}
                  </div>
                  {recipe.usesExpiringIngredients && (
                    <p className="text-sm text-yellow-600 mb-2 flex items-center gap-1">
                      <AlertCircle size={14} />
                      Uses expiring ingredients
                    </p>
                  )}
                  <div className="flex gap-2">
                    {isRecipeSaved(recipe) ? (
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          const saved = savedRecipes.find(r => r.externalRecipeId === recipe.externalRecipeId)
                          if (saved) handleUnsaveRecipe(saved.id)
                        }}
                        className="flex-1 py-2 rounded flex items-center justify-center gap-2 bg-red-50 text-red-700 hover:bg-red-100 border border-red-200"
                      >
                        <X size={16} />
                        Remove from Saved
                      </button>
                    ) : (
                      <button
                        onClick={(e) => {
                          e.stopPropagation()
                          handleSaveRecipe(recipe)
                        }}
                        className="flex-1 py-2 rounded flex items-center justify-center gap-2 bg-primary-600 text-white hover:bg-primary-700"
                      >
                        <Save size={16} />
                        Save Recipe
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Empty state when nothing loaded yet */}
      {!suggestionsAttempted && displayRecipes.length === 0 && filteredSavedRecipes.length === 0 && (
        <div className="bg-gray-50 rounded-xl p-8 border border-gray-100 text-center">
          <Compass size={48} className="mx-auto text-gray-400 mb-3" />
          <h3 className="text-lg font-semibold text-gray-800 mb-1">Start exploring</h3>
          <p className="text-gray-500 text-sm max-w-md mx-auto mb-4">
            Click any cuisine above to browse recipes, or add pantry items and use Suggest from pantry for personalized ideas.
          </p>
          <div className="flex flex-wrap justify-center gap-2">
            <button
              onClick={() => handleDiscover('Italian', 'ALL', 'Italian')}
              className="px-4 py-2 rounded-lg bg-white border border-gray-200 text-gray-700 hover:bg-primary-50 hover:border-primary-200 font-medium text-sm"
            >
              Try Italian
            </button>
            <button
              onClick={() => handleDiscover(null, 'VEGETARIAN', 'Vegetarian')}
              className="px-4 py-2 rounded-lg bg-white border border-gray-200 text-gray-700 hover:bg-emerald-50 hover:border-emerald-200 font-medium text-sm"
            >
              Try Vegetarian
            </button>
            <Link
              to="/pantry"
              className="px-4 py-2 rounded-lg bg-primary-100 text-primary-700 hover:bg-primary-200 font-medium text-sm inline-flex items-center gap-1"
            >
              Add to Pantry
            </Link>
          </div>
        </div>
      )}

      {/* Saved Recipes */}
      <div>
        <h2 className="text-xl font-semibold mb-4 text-gray-900">Your saved recipes</h2>
        {filteredSavedRecipes.length === 0 ? (
          <div className="bg-gray-50 rounded-xl p-8 border border-gray-100 text-center">
            <BookOpen size={40} className="mx-auto text-gray-400 mb-2" />
            <p className="text-gray-600 font-medium">No saved recipes yet</p>
            <p className="text-sm text-gray-500 mt-1">Discover recipes above and click Save to add them here for quick access.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredSavedRecipes.map((recipe) => (
              <div
                key={recipe.id}
                onClick={() => handleRecipeClick(recipe)}
                className="bg-white rounded-lg shadow hover:shadow-lg transition-shadow cursor-pointer overflow-hidden"
              >
                <RecipeImage
                  src={recipe.imageUrl}
                  alt={recipe.recipeName}
                  className="w-full h-48 object-cover"
                />
                <div className="p-4">
                  <h3 className="font-semibold text-lg mb-1.5 text-gray-900">{recipe.recipeName}</h3>
                  <div className="mb-2">
                    <YouTubeRecipeLink recipeName={recipe.recipeName} size="sm" />
                  </div>
                  <p className="text-sm text-gray-600">
                    {recipe.prepTimeMinutes ?? '?'} min prep • {recipe.cookTimeMinutes ?? '?'} min cook
                  </p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Recipe Detail Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-[100] flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-xl shadow-2xl max-w-4xl w-full max-h-[90vh] overflow-y-auto my-8">
            {loadingDetails ? (
              <div className="p-8 text-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
                <p className="mt-4 text-gray-600">Loading recipe details...</p>
              </div>
            ) : selectedRecipe ? (
              <div className="p-6">
                {/* Modal Header */}
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <h2 className="text-2xl font-bold text-gray-900">{selectedRecipe.recipeName}</h2>
                    <div className="mt-2">
                      <YouTubeRecipeLink recipeName={selectedRecipe.recipeName} size="md" />
                    </div>
                  </div>
                  <button
                    onClick={handleCloseModal}
                    className="text-gray-400 hover:text-gray-600"
                  >
                    <X size={24} />
                  </button>
                </div>

                {/* Recipe Image */}
                <RecipeImage
                  src={selectedRecipe.imageUrl}
                  alt={selectedRecipe.recipeName}
                  className="w-full h-64 object-cover rounded-lg mb-6"
                />

                {/* Recipe Info */}
                <div className="grid grid-cols-3 gap-4 mb-6">
                  {selectedRecipe.prepTimeMinutes && (
                    <div className="flex items-center gap-2 text-gray-700">
                      <Clock size={18} />
                      <span>{selectedRecipe.prepTimeMinutes} min prep</span>
                    </div>
                  )}
                  {selectedRecipe.cookTimeMinutes && (
                    <div className="flex items-center gap-2 text-gray-700">
                      <Clock size={18} />
                      <span>{selectedRecipe.cookTimeMinutes} min cook</span>
                    </div>
                  )}
                  {selectedRecipe.servings && (
                    <div className="flex items-center gap-2 text-gray-700">
                      <Users size={18} />
                      <span>{selectedRecipe.servings} servings</span>
                    </div>
                  )}
                </div>

                {/* Ingredients Section */}
                <div className="mb-6">
                  <h3 className="text-xl font-semibold mb-4 text-gray-900 flex items-center gap-2">
                    <BookOpen size={20} />
                    Ingredients
                  </h3>
                  <div className="space-y-3">
                    {(selectedRecipe.ingredients || []).map((ingredient, index) => {
                      const status = ingredient.status || 'Not in Pantry'
                      const isMissing = status === 'Not in Pantry'
                      const isLowStock = status === 'Low Stock'
                      const isInsufficient = status === 'Insufficient'
                      
                      return (
                        <div
                          key={index}
                          className={`p-4 rounded-lg border-2 ${
                            isMissing
                              ? 'border-red-300 bg-red-50'
                              : isInsufficient
                              ? 'border-orange-300 bg-orange-50'
                              : isLowStock
                              ? 'border-yellow-300 bg-yellow-50'
                              : 'border-gray-200 bg-gray-50'
                          }`}
                        >
                          <div className="flex justify-between items-start">
                            <div className="flex-1">
                              <div className="flex items-center gap-2 mb-2">
                                <span className="font-semibold text-gray-900">{ingredient.name}</span>
                                <span className={`px-2 py-1 rounded text-xs font-medium ${getStatusColor(status)}`}>
                                  {status}
                                </span>
                              </div>
                              <div className="text-sm text-gray-600 space-y-1">
                                <p>
                                  Required: {ingredient.requiredQuantity || ingredient.quantity} {ingredient.requiredUnit || ingredient.unit || 'pieces'}
                                </p>
                                {ingredient.pantryQuantity !== undefined && (
                                  <p>
                                    In Pantry: {ingredient.pantryQuantity} {ingredient.pantryUnit}
                                  </p>
                                )}
                              </div>
                            </div>
                            {isMissing && (
                              <button
                                onClick={() => handleAddToShoppingListClick(ingredient)}
                                className="ml-4 px-3 py-1 bg-primary-600 text-white rounded hover:bg-primary-700 flex items-center gap-1 text-sm"
                              >
                                <ShoppingCart size={14} />
                                Shop
                              </button>
                            )}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </div>

                {/* Missing Ingredients Summary */}
                {selectedRecipe.missingIngredients?.length > 0 && (
                  <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
                    <h4 className="font-semibold text-red-900 mb-2">Missing Ingredients</h4>
                    <div className="space-y-2">
                      {selectedRecipe.missingIngredients.map((ingredient, index) => (
                        <div key={index} className="flex justify-between items-center">
                          <span className="text-red-800">
                            {ingredient.name} - {ingredient.requiredQuantity || ingredient.quantity} {ingredient.requiredUnit || ingredient.unit || 'pieces'}
                          </span>
                          <button
                            onClick={() => handleAddToShoppingListClick(ingredient)}
                            className="px-3 py-1 bg-primary-600 text-white rounded hover:bg-primary-700 flex items-center gap-1 text-sm"
                          >
                            <ShoppingCart size={14} />
                            Add to List
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Instructions Section */}
                {(selectedRecipe.instructions && selectedRecipe.instructions.trim().length > 0) ? (
                  <div className="mb-6">
                    <h3 className="text-xl font-semibold mb-4 text-gray-900">Instructions</h3>
                    <div className="prose max-w-none">
                      {selectedRecipe.instructions
                        .split(/\n\n+|\n(?=\d+\.)/)
                        .map((step, index) => {
                          const trimmed = step.trim().replace(/^\d+\.\s*/, '')
                          if (!trimmed) return null
                          return (
                            <div key={index} className="mb-4 flex gap-3">
                              <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary-600 text-white flex items-center justify-center font-semibold">
                                {index + 1}
                              </div>
                              <p className="flex-1 text-gray-700 leading-relaxed">{trimmed}</p>
                            </div>
                          )
                        })}
                    </div>
                  </div>
                ) : (
                  <div className="mb-6 p-4 bg-amber-50 border border-amber-200 rounded-lg">
                    <p className="text-amber-800 text-sm">Step-by-step instructions were not available for this recipe. You can still save it and look up cooking instructions online.</p>
                  </div>
                )}

                {/* Action Buttons */}
                <div className="flex gap-3">
                  <button
                    onClick={handleCookRecipe}
                    className="flex-1 bg-primary-600 text-white px-4 py-2 rounded-lg hover:bg-primary-700 flex items-center justify-center gap-2"
                  >
                    <ChefHat size={18} />
                    Cook Recipe
                  </button>
                  {selectedRecipe.id ? (
                    <button
                      onClick={() => handleUnsaveRecipe(selectedRecipe.id)}
                      className="px-4 py-2 rounded-lg flex items-center justify-center gap-2 bg-blue-600 text-white hover:bg-blue-700 border border-blue-600"
                    >
                      <Save size={18} />
                      Remove from Saved
                    </button>
                  ) : (
                    <button
                      onClick={() => handleSaveRecipe(selectedRecipe)}
                      className="px-4 py-2 rounded-lg flex items-center justify-center gap-2 bg-blue-600 text-white hover:bg-blue-700 border border-blue-600"
                    >
                      <Save size={18} />
                      Save Recipe
                    </button>
                  )}
                </div>
              </div>
            ) : null}
          </div>
        </div>
      )}

      {/* Add to shopping list confirmation modal */}
      {addToShopConfirmItem && (
        <div
          className="fixed inset-0 z-[110] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
          onClick={handleAddToShoppingListCancel}
        >
          <div
            className="bg-white rounded-2xl shadow-xl w-full max-w-sm p-6 border border-gray-200"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-lg font-semibold text-gray-900 mb-2">Add to shopping list?</h3>
            <p className="text-gray-600 text-sm mb-4">
              Are you sure you want to add &quot;{addToShopConfirmItem.name}&quot; to your shopping list?
            </p>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={handleAddToShoppingListConfirm}
                disabled={addToShopAdding}
                className="flex-1 px-4 py-2.5 rounded-xl bg-primary-600 text-white font-medium hover:bg-primary-700 disabled:opacity-70 transition-colors"
              >
                {addToShopAdding ? 'Adding…' : 'Confirm'}
              </button>
              <button
                type="button"
                onClick={handleAddToShoppingListCancel}
                disabled={addToShopAdding}
                className="flex-1 px-4 py-2.5 rounded-xl border border-gray-200 text-gray-700 hover:bg-gray-50 font-medium disabled:opacity-70"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default Recipes
