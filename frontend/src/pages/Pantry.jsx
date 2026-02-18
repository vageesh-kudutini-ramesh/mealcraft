import { useEffect, useState, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import axios from '../utils/axios'
import { Plus, Edit, Trash2, X, Package, AlertCircle, Sparkles, ChevronDown, RotateCcw, BookOpen, HelpCircle } from 'lucide-react'
import { useNotification } from '../contexts/NotificationContext'
import { refreshNotifications } from '../utils/notifications'
import { getLocalDateStr } from '../utils/date'
import { CUISINE_INGREDIENTS, CUISINE_LABELS, getRandomizedSubset, QUICK_ADD_DISPLAY_COUNT } from '../data/cuisineIngredients'
import SlidingTicker from '../components/common/SlidingTicker'

/**
 * Pantry Page
 * 
 * Pantry inventory management with categories, expiration tracking, and filters.
 * 
 * @author MealCraft Team
 */
const Pantry = () => {
  const [allItems, setAllItems] = useState([])
  const [expiringItems, setExpiringItems] = useState([])
  const [expiredItems, setExpiredItems] = useState([])
  const [lowStockItems, setLowStockItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchParams, setSearchParams] = useSearchParams()
  const filter = searchParams.get('filter')
  const { showSuccess, showError } = useNotification()

  // Current view items based on filter
  const items = !filter ? allItems : filter === 'expiring' ? expiringItems : filter === 'expired' ? expiredItems : lowStockItems
  
  // Modal state
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  // Quick-add by cuisine state
  const [showQuickAdd, setShowQuickAdd] = useState(false)
  const [selectedCuisine, setSelectedCuisine] = useState(null)
  const [displayedIngredients, setDisplayedIngredients] = useState([])
  const [formData, setFormData] = useState({
    itemName: '',
    quantity: '',
    unit: 'pieces',
    category: 'FRUITS_VEGETABLES',
    expirationDate: '',
    threshold: ''
  })
  const [formErrors, setFormErrors] = useState({})
  const [deleteConfirmItem, setDeleteConfirmItem] = useState(null)
  const [showPantryGuide, setShowPantryGuide] = useState(() => {
    try {
      return localStorage.getItem('pantry-how-it-works-dismissed') !== 'true'
    } catch {
      return true
    }
  })
  const headerRef = useRef(null)

  // Category options
  const categories = [
    { value: 'FRUITS_VEGETABLES', label: '🥕 Fruits & Vegetables' },
    { value: 'DAIRY_PRODUCTS', label: '🥛 Dairy Products' },
    { value: 'PANTRY_STAPLES', label: '🍚 Pantry Staples' },
    { value: 'CONDIMENTS_SPICES', label: '🧂 Condiments & Spices' }
  ]

  // Unit options
  const units = [
    'pieces', 'grams', 'kg', 'ml', 'liters', 'cups', 'tbsp', 'tsp', 'oz', 'lbs'
  ]

  useEffect(() => {
    fetchPantryItems()
  }, [])

  useEffect(() => {
    headerRef.current?.scrollIntoView({ block: 'start' })
  }, [filter])

  // When cuisine is selected, load a random subset of ingredients
  useEffect(() => {
    if (selectedCuisine && CUISINE_INGREDIENTS[selectedCuisine]) {
      setDisplayedIngredients(getRandomizedSubset(CUISINE_INGREDIENTS[selectedCuisine], QUICK_ADD_DISPLAY_COUNT))
    } else {
      setDisplayedIngredients([])
    }
  }, [selectedCuisine])

  const handleRefreshIngredients = () => {
    if (selectedCuisine && CUISINE_INGREDIENTS[selectedCuisine]) {
      setDisplayedIngredients(getRandomizedSubset(CUISINE_INGREDIENTS[selectedCuisine], QUICK_ADD_DISPLAY_COUNT))
    }
  }

  const fetchPantryItems = async () => {
    try {
      setLoading(true)
      const localDate = getLocalDateStr()
      const [allRes, expiringRes, expiredRes, lowStockRes] = await Promise.all([
        axios.get('/api/pantry'),
        axios.get(`/api/pantry/expiring?localDate=${localDate}`),
        axios.get(`/api/pantry/expired?localDate=${localDate}`),
        axios.get('/api/pantry/low-stock')
      ])
      setAllItems(allRes.data || [])
      setExpiringItems(expiringRes.data || [])
      setExpiredItems(expiredRes.data || [])
      setLowStockItems(lowStockRes.data || [])
    } catch (error) {
      showError('Error loading pantry items')
      setAllItems([])
      setExpiringItems([])
      setExpiredItems([])
      setLowStockItems([])
    } finally {
      setLoading(false)
    }
  }

  const handleAddClick = (presetItemName = '') => {
    setEditingItem(null)
    setFormData({
      itemName: presetItemName,
      quantity: '',
      unit: 'pieces',
      category: 'FRUITS_VEGETABLES',
      expirationDate: '',
      threshold: ''
    })
    setFormErrors({})
    setIsModalOpen(true)
    // If opened from quick-add, collapse it
    setShowQuickAdd(false)
    setSelectedCuisine(null)
  }

  const handleQuickAddIngredient = (ingredientName) => {
    handleAddClick(ingredientName)
  }

  // Helper function to format date for date input (YYYY-MM-DD)
  // Extracts date part from string to avoid timezone issues
  const formatDateForInput = (dateString) => {
    if (!dateString) return ''
    // If it's already in YYYY-MM-DD format, return as is
    if (typeof dateString === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(dateString)) {
      return dateString
    }
    // If it's a date string with time, extract just the date part
    if (typeof dateString === 'string' && dateString.includes('T')) {
      return dateString.split('T')[0]
    }
    // If it's a date string with space, extract just the date part
    if (typeof dateString === 'string' && dateString.includes(' ')) {
      return dateString.split(' ')[0]
    }
    // Fallback: try to parse and extract date components
    // Parse as local date to avoid timezone shift
    const date = new Date(dateString)
    // Check if date is valid
    if (isNaN(date.getTime())) {
      return ''
    }
    // Use local date components (not UTC) to preserve the selected date
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  // Helper function to format date for display
  // Parses date string directly as local date to avoid timezone issues
  const formatDateForDisplay = (dateString) => {
    if (!dateString) return ''
    // Extract date part if it includes time
    let dateOnly = dateString
    if (typeof dateString === 'string') {
      if (dateString.includes('T')) {
        dateOnly = dateString.split('T')[0]
      } else if (dateString.includes(' ')) {
        dateOnly = dateString.split(' ')[0]
      }
    }
    
    // Parse as local date (not UTC) to preserve the correct date
    if (/^\d{4}-\d{2}-\d{2}$/.test(dateOnly)) {
      const [year, month, day] = dateOnly.split('-').map(Number)
      // Create date using local timezone (not UTC)
      const date = new Date(year, month - 1, day)
      return date.toLocaleDateString('en-US', { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric' 
      })
    }
    
    // Fallback
    return dateString
  }

  const handleEditClick = (item) => {
    setEditingItem(item)
    setFormData({
      itemName: item.itemName,
      quantity: item.quantity.toString(),
      unit: item.unit,
      category: item.category,
      expirationDate: formatDateForInput(item.expirationDate),
      threshold: item.threshold.toString()
    })
    setFormErrors({})
    setIsModalOpen(true)
  }

  const handleCloseModal = () => {
    setIsModalOpen(false)
    setEditingItem(null)
    setFormData({
      itemName: '',
      quantity: '',
      unit: 'pieces',
      category: 'FRUITS_VEGETABLES',
      expirationDate: '',
      threshold: ''
    })
    setFormErrors({})
  }

  const validateForm = () => {
    const errors = {}
    
    if (!formData.itemName.trim()) {
      errors.itemName = 'Item name is required'
    }
    
    if (!formData.quantity || parseFloat(formData.quantity) <= 0) {
      errors.quantity = 'Quantity must be greater than 0'
    }
    
    if (!formData.unit) {
      errors.unit = 'Unit is required'
    }
    
    if (!formData.category) {
      errors.category = 'Category is required'
    }
    
    if (!formData.expirationDate) {
      errors.expirationDate = 'Expiration date is required'
    }
    
    if (!formData.threshold || parseFloat(formData.threshold) <= 0) {
      errors.threshold = 'Threshold must be greater than 0'
    }
    
    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    if (!validateForm()) {
      return
    }

    try {
      const payload = {
        itemName: formData.itemName.trim(),
        quantity: parseFloat(formData.quantity),
        unit: formData.unit,
        category: formData.category,
        expirationDate: formData.expirationDate,
        threshold: parseFloat(formData.threshold)
      }

      if (editingItem) {
        await axios.put(`/api/pantry/${editingItem.id}`, payload)
        showSuccess('Item updated successfully')
      } else {
        await axios.post('/api/pantry', payload)
        showSuccess('Item added successfully')
      }
      
      handleCloseModal()
      fetchPantryItems()
    } catch (error) {
      const errorMessage = error.response?.data?.message || 
        (editingItem ? 'Error updating item' : 'Error adding item')
      showError(errorMessage)
    }
  }

  const handleDelete = async (id) => {
    try {
      await axios.delete(`/api/pantry/${id}`)
      showSuccess('Item deleted successfully')
      refreshNotifications()
      fetchPantryItems()
      setDeleteConfirmItem(null)
    } catch (error) {
      showError('Error deleting item')
    }
  }

  const handleFilterChange = (newFilter) => {
    if (newFilter) {
      setSearchParams({ filter: newFilter })
    } else {
      setSearchParams({})
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'FRESH':
        return 'bg-green-100 text-green-800 border-green-200'
      case 'EXPIRING_SOON':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200'
      case 'EXPIRED':
        return 'bg-red-100 text-red-800 border-red-200'
      case 'LOW_STOCK':
        return 'bg-orange-100 text-orange-800 border-orange-200'
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200'
    }
  }

  const getStatusLabel = (status) => {
    switch (status) {
      case 'FRESH':
        return '🟢 Fresh'
      case 'EXPIRING_SOON':
        return '🟡 Expiring Soon'
      case 'EXPIRED':
        return '🔴 Expired'
      case 'LOW_STOCK':
        return '🟠 Low Stock'
      default:
        return status || '—'
    }
  }

  const filterTabs = [
    { key: null, label: 'All Items', count: allItems.length, activeClass: 'bg-primary-600 text-white shadow-sm border-2 border-primary-600', inactiveClass: 'text-gray-600 hover:bg-gray-100 border-2 border-transparent' },
    { key: 'expiring', label: 'Expiring Soon', count: expiringItems.length, activeClass: 'bg-yellow-100 text-yellow-800 border-2 border-yellow-300', inactiveClass: 'text-gray-600 hover:bg-gray-100 border-2 border-transparent' },
    { key: 'expired', label: 'Expired', count: expiredItems.length, activeClass: 'bg-red-100 text-red-800 border-2 border-red-300', inactiveClass: 'text-gray-600 hover:bg-gray-100 border-2 border-transparent' },
    { key: 'low-stock', label: 'Low Stock', count: lowStockItems.length, activeClass: 'bg-orange-100 text-orange-800 border-2 border-orange-300', inactiveClass: 'text-gray-600 hover:bg-gray-100 border-2 border-transparent' }
  ]

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading pantry...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6 overflow-x-hidden min-w-0 w-full max-w-full">
      {/* Pantry Guide – collapsible, like Meal Plan */}
      {showPantryGuide ? (
        <div className="bg-gradient-to-br from-primary-50 to-blue-50/50 rounded-2xl border border-primary-100 p-6 shadow-soft">
          <div className="flex items-start gap-4">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary-100">
              <BookOpen className="h-5 w-5 text-primary-600" />
            </div>
            <div className="flex-1 min-w-0">
              <h3 className="text-base font-semibold text-gray-900 mb-3">How to use your Pantry</h3>
              <ol className="space-y-2 text-sm text-gray-700">
                <li className="flex gap-2">
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary-200 text-primary-700 text-xs font-bold">1</span>
                  <span><strong>Quick Add from Suggestions</strong> – pick a cuisine (Indian, Italian, etc.), then choose ingredients from the list. Click Refresh to see new suggestions. Set quantity, unit, category, expiration date, and low-stock threshold when adding.</span>
                </li>
                <li className="flex gap-2">
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary-200 text-primary-700 text-xs font-bold">2</span>
                  <span><strong>Add Item</strong> – add any ingredient manually. Use filters (All, Expiring Soon, Expired, Low Stock) to track what needs attention.</span>
                </li>
                <li className="flex gap-2">
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary-200 text-primary-700 text-xs font-bold">3</span>
                  <span><strong>Track smartly</strong> – expiration dates help you reduce waste. Low-stock thresholds remind you when to restock.</span>
                </li>
              </ol>
              <button
                type="button"
                onClick={() => { setShowPantryGuide(false); try { localStorage.setItem('pantry-how-it-works-dismissed', 'true') } catch {} }}
                className="mt-4 inline-flex items-center gap-2 px-4 py-2.5 rounded-lg font-medium bg-primary-600 text-white hover:bg-primary-700 transition-colors shadow-sm"
              >
                Got it, thanks →
              </button>
            </div>
          </div>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setShowPantryGuide(true)}
          className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-medium bg-primary-50 text-primary-700 border-2 border-primary-200 hover:bg-primary-100 hover:border-primary-300 transition-all shadow-sm"
        >
          <HelpCircle size={20} />
          View guide – How to use your Pantry
        </button>
      )}

      {/* Header + Tabs – same layout for all tabs including Low Stock */}
      <div ref={headerRef} className="pb-3 border-b border-gray-100">
        <div className="flex flex-wrap justify-between items-center gap-3 mb-3">
          <p className="text-gray-600">Manage your pantry inventory and track expiration dates</p>
          <div className="flex flex-wrap items-center gap-3">
            <button
              onClick={() => setShowQuickAdd(!showQuickAdd)}
              className={`px-5 py-3 rounded-lg flex items-center font-medium transition-all shrink-0 ${
                showQuickAdd
                  ? 'bg-primary-600 text-white shadow-md'
                  : 'bg-primary-50 text-primary-700 border-2 border-primary-200 hover:bg-primary-100'
              }`}
            >
              <Sparkles size={20} className="mr-2" />
              Quick Add from Suggestions
              <ChevronDown size={18} className={`ml-2 transition-transform ${showQuickAdd ? 'rotate-180' : ''}`} />
            </button>
            <button
              onClick={() => handleAddClick()}
              className="bg-primary-600 text-white px-6 py-3 rounded-lg hover:bg-primary-700 flex items-center shadow-md hover:shadow-lg transition-all font-medium shrink-0"
            >
              <Plus size={20} className="mr-2" />
              Add Item
            </button>
          </div>
        </div>

        {/* Filter Tabs – flex-nowrap keeps same height for all tabs; scroll if needed */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-1 shrink-0 overflow-x-auto">
          <div className="flex flex-nowrap gap-1">
            {filterTabs.map((tab) => {
              const isActive = (tab.key === null && !filter) || (tab.key === filter)
              return (
                <button
                  key={tab.key ?? 'all'}
                  type="button"
                  onClick={() => handleFilterChange(tab.key)}
                  className={`shrink-0 px-3 sm:px-4 py-3 rounded-md font-medium text-sm sm:text-base ${
                    isActive ? tab.activeClass : tab.inactiveClass
                  }`}
                  style={{ minWidth: 100 }}
                >
                  {tab.label} <span className="ml-1 text-sm opacity-90">({tab.count})</span>
                </button>
              )
            })}
          </div>
        </div>
      </div>

      {/* Quick Add by Cuisine */}
      {showQuickAdd && (
        <div className="bg-white rounded-xl shadow-md border border-slate-200 overflow-hidden animate-fade-in">
          <div className="p-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-3">Choose a cuisine or region to see suggested ingredients</h3>
            <div className="flex flex-wrap gap-2 mb-4">
              {CUISINE_LABELS.map((cuisine) => (
                <button
                  key={cuisine}
                  onClick={() => setSelectedCuisine(selectedCuisine === cuisine ? null : cuisine)}
                  className={`px-4 py-2 rounded-lg font-medium transition-all ${
                    selectedCuisine === cuisine
                      ? 'bg-primary-600 text-white shadow-md'
                      : 'bg-slate-100 text-slate-700 hover:bg-primary-100 hover:text-primary-800'
                  }`}
                >
                  {cuisine}
                </button>
              ))}
            </div>
            {selectedCuisine && (
              <>
                <div className="flex flex-wrap items-center gap-3 mb-3">
                  <p className="text-sm text-gray-600 flex-1 min-w-0">
                    Click an ingredient to add it to your pantry. You&apos;ll then set quantity, unit, category, expiration date, and threshold.
                  </p>
                  <button
                    onClick={handleRefreshIngredients}
                    className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-primary-100 text-primary-700 font-medium hover:bg-primary-200 transition-all border border-primary-200 shadow-sm"
                    title="Load different ingredient suggestions"
                  >
                    <RotateCcw size={18} />
                    Refresh – see new items
                  </button>
                </div>
                <p className="text-xs text-primary-600 mb-2">
                  Showing {displayedIngredients.length} of {CUISINE_INGREDIENTS[selectedCuisine].length} items · Click refresh for more suggestions
                </p>
                <div className="flex flex-wrap gap-2">
                  {displayedIngredients.map((ingredient) => (
                    <button
                      key={ingredient}
                      onClick={() => handleQuickAddIngredient(ingredient)}
                      className="px-4 py-2 bg-primary-50 text-primary-800 rounded-lg font-medium hover:bg-primary-100 hover:shadow-sm transition-all border border-primary-100"
                    >
                      {ingredient}
                    </button>
                  ))}
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* Items List – same layout for all columns (grid or empty state) */}
      {items.length === 0 ? (
        <div className="bg-white rounded-xl shadow-md border border-gray-200 overflow-hidden">
          <div className="p-12 text-center">
            <Package className="mx-auto text-gray-400 mb-4" size={64} />
            <h3 className="text-xl font-semibold text-gray-700 mb-2">
              {filter === 'expiring' && 'No items expiring soon'}
              {filter === 'expired' && 'No expired items'}
              {filter === 'low-stock' && 'No low stock items'}
              {!filter && 'Your pantry is empty'}
            </h3>
            <p className="text-gray-500 mb-6">
              {!filter && 'Start by adding items to your pantry to track expiration dates and manage inventory.'}
              {filter === 'expiring' && 'No items expire in the next 7 days.'}
              {filter === 'expired' && 'No items have passed their expiration date.'}
              {filter === 'low-stock' && 'All items are above their low-stock threshold.'}
            </p>
            {filter !== 'low-stock' && (
              <div className="flex flex-wrap justify-center gap-3">
                <button
                  onClick={() => { setShowQuickAdd(true); setSelectedCuisine(null); }}
                  className="bg-primary-50 text-primary-700 border-2 border-primary-200 px-6 py-3 rounded-lg hover:bg-primary-100 inline-flex items-center font-medium"
                >
                  <Sparkles size={20} className="mr-2" />
                  Quick Add from Suggestions
                </button>
                <button
                  onClick={() => handleAddClick()}
                  className="bg-primary-600 text-white px-6 py-3 rounded-lg hover:bg-primary-700 inline-flex items-center font-medium"
                >
                  <Plus size={20} className="mr-2" />
                  {!filter ? 'Add Your First Item' : 'Add Item'}
                </button>
              </div>
            )}
          </div>
          <SlidingTicker variant="default" />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 min-w-0">
          {items.map((item) => (
            <div
              key={item.id}
              className="bg-white rounded-lg shadow-md border border-gray-200 p-6 hover:shadow-lg transition-all min-w-0 overflow-hidden"
            >
              {/* Header */}
              <div className="flex justify-between items-start mb-4">
                <div className="flex-1 min-w-0">
                  <h3 className="font-bold text-lg text-gray-900 mb-1 break-words">{item.itemName}</h3>
                  <span className={`inline-block px-3 py-1 rounded-full text-xs font-semibold border ${getStatusColor(item.expirationStatus)}`}>
                    {getStatusLabel(item.expirationStatus)}
                  </span>
                </div>
              </div>

              {/* Details */}
              <div className="space-y-2 mb-4">
                <div className="flex items-center text-gray-700">
                  <Package size={16} className="mr-2 text-gray-500" />
                  <span className="font-medium">
                    {item.quantity} {item.unit}
                  </span>
                </div>
                <div className="flex items-center text-gray-700">
                  <span className="text-sm">
                    📅 Expires: <span className="font-medium">{formatDateForDisplay(item.expirationDate)}</span>
                  </span>
                </div>
                {item.daysUntilExpiry !== undefined && (
                  <div className="flex items-center">
                    {item.daysUntilExpiry > 0 ? (
                      <span className="text-sm text-yellow-700 font-medium">
                        ⏰ {item.daysUntilExpiry} {item.daysUntilExpiry === 1 ? 'day' : 'days'} until expiry
                      </span>
                    ) : (
                      <span className="text-sm text-red-700 font-medium">
                        ⚠️ Expired {Math.abs(item.daysUntilExpiry)} {Math.abs(item.daysUntilExpiry) === 1 ? 'day' : 'days'} ago
                      </span>
                    )}
                  </div>
                )}
                {item.isLowStock && (
                  <div className="flex items-center text-orange-700 min-w-0">
                    <AlertCircle size={16} className="mr-2 shrink-0" />
                    <span className="text-sm font-medium break-words">Low stock (threshold: {item.threshold} {item.unit})</span>
                  </div>
                )}
                <div className="text-sm text-gray-500 pt-2 border-t border-gray-100">
                  Category: {categories.find(c => c.value === item.category)?.label || item.category}
                </div>
              </div>

              {/* Actions */}
              <div className="flex space-x-2 pt-4 border-t border-gray-100">
                <button
                  onClick={() => handleEditClick(item)}
                  className="flex-1 bg-primary-50 text-primary-700 px-4 py-2 rounded-lg hover:bg-primary-100 flex items-center justify-center font-medium transition-colors"
                >
                  <Edit size={16} className="mr-2" />
                  Edit
                </button>
                <button
                  onClick={() => setDeleteConfirmItem({ id: item.id, itemName: item.itemName })}
                  className="flex-1 bg-red-50 text-red-700 px-4 py-2 rounded-lg hover:bg-red-100 flex items-center justify-center font-medium transition-colors"
                >
                  <Trash2 size={16} className="mr-2" />
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Delete confirmation modal */}
      {deleteConfirmItem && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
          onClick={() => setDeleteConfirmItem(null)}
        >
          <div
            className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 border border-gray-200"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center">
                <Trash2 size={24} className="text-red-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900">Delete pantry item?</h3>
            </div>
            <p className="text-gray-600 mb-6">
              Are you sure you want to delete &quot;{deleteConfirmItem.itemName}&quot;? This cannot be undone.
            </p>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => handleDelete(deleteConfirmItem.id)}
                className="flex-1 px-4 py-2.5 rounded-xl bg-red-600 text-white font-medium hover:bg-red-700 transition-colors"
              >
                Delete
              </button>
              <button
                type="button"
                onClick={() => setDeleteConfirmItem(null)}
                className="flex-1 px-4 py-2.5 rounded-xl border border-gray-200 text-gray-700 hover:bg-gray-50 font-medium"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add/Edit Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            {/* Modal Header */}
            <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center">
              <h2 className="text-2xl font-bold text-gray-900">
                {editingItem ? 'Edit Pantry Item' : 'Add New Pantry Item'}
              </h2>
              <button
                onClick={handleCloseModal}
                className="text-gray-400 hover:text-gray-600 transition-colors"
              >
                <X size={24} />
              </button>
            </div>

            {/* Modal Body */}
            <form onSubmit={handleSubmit} className="p-6 space-y-6">
              {/* Item Name */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Item Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.itemName}
                  onChange={(e) => setFormData({ ...formData, itemName: e.target.value })}
                  className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-gray-900 ${
                    formErrors.itemName ? 'border-red-500' : 'border-gray-300'
                  }`}
                  placeholder="e.g., Milk, Tomatoes, Rice"
                />
                {formErrors.itemName && (
                  <p className="text-red-500 text-sm mt-1">{formErrors.itemName}</p>
                )}
              </div>

              {/* Quantity and Unit */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    Quantity <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={formData.quantity}
                    onChange={(e) => setFormData({ ...formData, quantity: e.target.value })}
                    className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-gray-900 ${
                      formErrors.quantity ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="0.00"
                  />
                  {formErrors.quantity && (
                    <p className="text-red-500 text-sm mt-1">{formErrors.quantity}</p>
                  )}
                </div>
                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-2">
                    Unit <span className="text-red-500">*</span>
                  </label>
                  <select
                    value={formData.unit}
                    onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                    className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-gray-900 bg-white ${
                      formErrors.unit ? 'border-red-500' : 'border-gray-300'
                    }`}
                  >
                    {units.map((unit) => (
                      <option key={unit} value={unit}>
                        {unit}
                      </option>
                    ))}
                  </select>
                  {formErrors.unit && (
                    <p className="text-red-500 text-sm mt-1">{formErrors.unit}</p>
                  )}
                </div>
              </div>

              {/* Category */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Category <span className="text-red-500">*</span>
                </label>
                <select
                  value={formData.category}
                  onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                  className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-gray-900 bg-white ${
                    formErrors.category ? 'border-red-500' : 'border-gray-300'
                  }`}
                >
                  {categories.map((cat) => (
                    <option key={cat.value} value={cat.value}>
                      {cat.label}
                    </option>
                  ))}
                </select>
                {formErrors.category && (
                  <p className="text-red-500 text-sm mt-1">{formErrors.category}</p>
                )}
              </div>

              {/* Expiration Date */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Expiration Date <span className="text-red-500">*</span>
                </label>
                <input
                  type="date"
                  value={formData.expirationDate}
                  onChange={(e) => setFormData({ ...formData, expirationDate: e.target.value })}
                  className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-gray-900 ${
                    formErrors.expirationDate ? 'border-red-500' : 'border-gray-300'
                  }`}
                />
                {formErrors.expirationDate && (
                  <p className="text-red-500 text-sm mt-1">{formErrors.expirationDate}</p>
                )}
              </div>

              {/* Threshold */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                  Low Stock Threshold <span className="text-red-500">*</span>
                  <span className="text-xs font-normal text-gray-500 ml-2">
                    (Alert when quantity falls below this value)
                  </span>
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={formData.threshold}
                  onChange={(e) => setFormData({ ...formData, threshold: e.target.value })}
                  className={`w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-gray-900 ${
                    formErrors.threshold ? 'border-red-500' : 'border-gray-300'
                  }`}
                  placeholder="0.00"
                />
                {formErrors.threshold && (
                  <p className="text-red-500 text-sm mt-1">{formErrors.threshold}</p>
                )}
              </div>

              {/* Modal Footer */}
              <div className="flex space-x-4 pt-4 border-t border-gray-200">
                <button
                  type="button"
                  onClick={handleCloseModal}
                  className="flex-1 px-6 py-3 border border-gray-300 rounded-lg text-gray-700 font-medium hover:bg-gray-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 px-6 py-3 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700 transition-colors shadow-md"
                >
                  {editingItem ? 'Update Item' : 'Add Item'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

export default Pantry
