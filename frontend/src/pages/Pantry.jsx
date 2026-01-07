import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import axios from '../utils/axios'
import { Plus, Edit, Trash2, X, Package, AlertCircle } from 'lucide-react'
import { useNotification } from '../contexts/NotificationContext'

/**
 * Pantry Page
 * 
 * Pantry inventory management with categories, expiration tracking, and filters.
 * 
 * @author MealCraft Team
 */
const Pantry = () => {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [searchParams, setSearchParams] = useSearchParams()
  const filter = searchParams.get('filter')
  const { showSuccess, showError } = useNotification()
  
  // Modal state
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState(null)
  const [formData, setFormData] = useState({
    itemName: '',
    quantity: '',
    unit: 'pieces',
    category: 'FRUITS_VEGETABLES',
    expirationDate: '',
    threshold: ''
  })
  const [formErrors, setFormErrors] = useState({})

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
  }, [filter])

  const fetchPantryItems = async () => {
    try {
      setLoading(true)
      let response
      if (filter === 'expiring') {
        response = await axios.get('/api/pantry/expiring')
      } else if (filter === 'expired') {
        response = await axios.get('/api/pantry/expired')
      } else if (filter === 'low-stock') {
        response = await axios.get('/api/pantry/low-stock')
      } else {
        response = await axios.get('/api/pantry')
      }
      setItems(response.data)
    } catch (error) {
      showError('Error loading pantry items')
    } finally {
      setLoading(false)
    }
  }

  const handleAddClick = () => {
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
    setIsModalOpen(true)
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
    if (!window.confirm('Are you sure you want to delete this item?')) return

    try {
      await axios.delete(`/api/pantry/${id}`)
      showSuccess('Item deleted successfully')
      fetchPantryItems()
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
      default:
        return status
    }
  }

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
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">My Pantry</h1>
          <p className="text-gray-600 mt-1">Manage your pantry inventory and track expiration dates</p>
        </div>
        <button
          onClick={handleAddClick}
          className="bg-primary-600 text-white px-6 py-3 rounded-lg hover:bg-primary-700 flex items-center shadow-md hover:shadow-lg transition-all font-medium"
        >
          <Plus size={20} className="mr-2" />
          Add Item
        </button>
      </div>

      {/* Filter Tabs */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-1">
        <div className="flex space-x-1">
          <button
            onClick={() => handleFilterChange(null)}
            className={`flex-1 px-4 py-3 rounded-md font-medium transition-all ${
              !filter
                ? 'bg-primary-600 text-white shadow-sm'
                : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            All Items
            {!filter && <span className="ml-2 text-sm">({items.length})</span>}
          </button>
          <button
            onClick={() => handleFilterChange('expiring')}
            className={`flex-1 px-4 py-3 rounded-md font-medium transition-all ${
              filter === 'expiring'
                ? 'bg-yellow-100 text-yellow-800 border border-yellow-300'
                : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            Expiring Soon
            {filter === 'expiring' && <span className="ml-2 text-sm">({items.length})</span>}
          </button>
          <button
            onClick={() => handleFilterChange('expired')}
            className={`flex-1 px-4 py-3 rounded-md font-medium transition-all ${
              filter === 'expired'
                ? 'bg-red-100 text-red-800 border border-red-300'
                : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            Expired
            {filter === 'expired' && <span className="ml-2 text-sm">({items.length})</span>}
          </button>
          <button
            onClick={() => handleFilterChange('low-stock')}
            className={`flex-1 px-4 py-3 rounded-md font-medium transition-all ${
              filter === 'low-stock'
                ? 'bg-orange-100 text-orange-800 border border-orange-300'
                : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            Low Stock
            {filter === 'low-stock' && <span className="ml-2 text-sm">({items.length})</span>}
          </button>
        </div>
      </div>

      {/* Items List */}
      {items.length === 0 ? (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-12 text-center">
          <Package className="mx-auto text-gray-400 mb-4" size={64} />
          <h3 className="text-xl font-semibold text-gray-700 mb-2">
            {filter === 'expiring' && 'No items expiring soon'}
            {filter === 'expired' && 'No expired items'}
            {filter === 'low-stock' && 'No low stock items'}
            {!filter && 'Your pantry is empty'}
          </h3>
          <p className="text-gray-500 mb-6">
            {!filter && 'Start by adding items to your pantry to track expiration dates and manage inventory.'}
            {filter && 'Great job! All items are in good condition.'}
          </p>
          {!filter && (
            <button
              onClick={handleAddClick}
              className="bg-primary-600 text-white px-6 py-3 rounded-lg hover:bg-primary-700 inline-flex items-center font-medium"
            >
              <Plus size={20} className="mr-2" />
              Add Your First Item
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {items.map((item) => (
            <div
              key={item.id}
              className="bg-white rounded-lg shadow-md border border-gray-200 p-6 hover:shadow-lg transition-all"
            >
              {/* Header */}
              <div className="flex justify-between items-start mb-4">
                <div className="flex-1">
                  <h3 className="font-bold text-lg text-gray-900 mb-1">{item.itemName}</h3>
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
                  <div className="flex items-center text-orange-700">
                    <AlertCircle size={16} className="mr-2" />
                    <span className="text-sm font-medium">Low stock (threshold: {item.threshold} {item.unit})</span>
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
                  onClick={() => handleDelete(item.id)}
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
