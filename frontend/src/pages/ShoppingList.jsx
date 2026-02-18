import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { format } from 'date-fns'
import axios from '../utils/axios'
import { Plus, Check, Trash2, ShoppingCart, Calendar, ChevronDown, ChevronUp, X } from 'lucide-react'
import { useNotification } from '../contexts/NotificationContext'
import { refreshNotifications } from '../utils/notifications'
import { getLocalDateStr } from '../utils/date'
import SlidingTicker from '../components/common/SlidingTicker'

/** Format yyyy-MM-dd as local date (avoids timezone shift – e.g. Eastern user sees correct local date) */
const formatDate = (dateStr) => {
  if (!dateStr) return null
  try {
    const parts = String(dateStr).split('-')
    if (parts.length !== 3) {
      const d = new Date(dateStr)
      return isNaN(d.getTime()) ? null : format(d, 'MMM d, yyyy')
    }
    const y = parseInt(parts[0], 10)
    const m = parseInt(parts[1], 10) - 1
    const d = parseInt(parts[2], 10)
    const date = new Date(y, m, d)
    return isNaN(date.getTime()) ? null : format(date, 'MMM d, yyyy')
  } catch {
    return null
  }
}

const UNITS = ['pieces', 'grams', 'kg', 'ml', 'liters', 'cups', 'tbsp', 'tsp', 'oz', 'lb']

/**
 * Shopping List Page – to buy, purchased, add from Meal Plan, manual add.
 */
const ShoppingList = () => {
  const [toBuy, setToBuy] = useState([])
  const [purchased, setPurchased] = useState([])
  const [loading, setLoading] = useState(true)
  const [showPurchased, setShowPurchased] = useState(false)
  const [showAddModal, setShowAddModal] = useState(false)
  const [addName, setAddName] = useState('')
  const [addQty, setAddQty] = useState('1')
  const [addUnit, setAddUnit] = useState('pieces')
  const [adding, setAdding] = useState(false)
  const [deleteConfirmItem, setDeleteConfirmItem] = useState(null) // { id, itemName }
  const [clearConfirmSection, setClearConfirmSection] = useState(null) // 'toBuy' | 'purchased'
  const { showSuccess, showError } = useNotification()

  useEffect(() => {
    fetchAll()
  }, [])

  const fetchAll = async () => {
    try {
      setLoading(true)
      const [unpurchased, all] = await Promise.all([
        axios.get('/api/shopping-list/unpurchased'),
        axios.get('/api/shopping-list')
      ])
      setToBuy(unpurchased.data || [])
      const purchasedList = (all.data || []).filter((i) => i.isPurchased)
      setPurchased(purchasedList)
    } catch (error) {
      showError('Error loading shopping list')
      setToBuy([])
      setPurchased([])
    } finally {
      setLoading(false)
    }
  }

  const handleMarkPurchased = async (id) => {
    try {
      await axios.post(`/api/shopping-list/${id}/purchase`)
      showSuccess('Marked as purchased')
      fetchAll()
    } catch (error) {
      showError('Error updating item')
    }
  }

  const handleDelete = async (id) => {
    try {
      await axios.delete(`/api/shopping-list/${id}`)
      showSuccess('Item removed')
      fetchAll()
    } catch (error) {
      showError('Error removing item')
    }
  }

  const handleClearPurchased = async () => {
    try {
      await axios.delete('/api/shopping-list/purchased')
      showSuccess('Purchased items cleared')
      fetchAll()
    } catch (error) {
      showError('Error clearing items')
    }
  }

  const handleAddItem = async (e) => {
    e.preventDefault()
    const name = addName.trim()
    if (!name) {
      showError('Enter item name')
      return
    }
    const qty = parseFloat(addQty)
    if (isNaN(qty) || qty <= 0) {
      showError('Enter a valid quantity')
      return
    }
    setAdding(true)
    try {
      await axios.post('/api/shopping-list', {
        itemName: name,
        quantity: qty,
        unit: addUnit,
        isPurchased: false,
        addedAt: getLocalDateStr()
      })
      showSuccess('Item added')
      refreshNotifications()
      setAddName('')
      setAddQty('1')
      setAddUnit('pieces')
      setShowAddModal(false)
      fetchAll()
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message || 'Failed to add item'
      showError(msg)
    } finally {
      setAdding(false)
    }
  }

  const formatQuantity = (q) => {
    if (Number.isInteger(q)) return String(q)
    const r = Math.round(q * 100) / 100
    return r % 1 === 0 ? String(r) : r.toFixed(2)
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[40vh]">
        <motion.div
          animate={{ rotate: 360 }}
          transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
          className="w-10 h-10 border-2 border-primary-500 border-t-transparent rounded-full"
        />
      </div>
    )
  }

  const isEmpty = toBuy.length === 0 && purchased.length === 0

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl sm:text-3xl font-display font-bold text-slate-900">Shopping List</h1>
      </motion.div>

      {/* Empty state – engaging */}
      {isEmpty && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-emerald-500/10 via-accent-50/50 to-primary-50 border border-slate-200/80"
        >
          <div className="p-8 sm:p-10 text-center">
            <div className="inline-flex items-center justify-center w-20 h-20 rounded-2xl bg-gradient-to-br from-emerald-400 to-primary-500 shadow-card mb-5">
              <ShoppingCart className="h-10 w-10 text-white" />
            </div>
            <h2 className="text-xl font-display font-bold text-slate-900 mb-2">Your list is empty</h2>
            <p className="text-slate-600 mb-6 max-w-md mx-auto">
              Plan your meals, then add ingredients in one click. Or add items manually for quick pickups.
            </p>
            <div className="flex flex-wrap justify-center gap-3">
              <Link to="/meal-plan" className="btn-primary inline-flex items-center gap-2">
                <Calendar size={18} />
                Add from Meal Plan
              </Link>
              <motion.button
                onClick={() => setShowAddModal(true)}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl border-2 border-primary-300 text-primary-600 font-medium hover:bg-primary-50 transition-colors"
              >
                <Plus size={18} />
                Add item manually
              </motion.button>
            </div>
          </div>
          <SlidingTicker variant="default" />
        </motion.div>
      )}

      {/* To buy */}
      {toBuy.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="card overflow-hidden"
        >
          <div className="px-5 py-4 border-b border-gray-100 bg-gray-50/50 flex items-center justify-between">
            <h2 className="text-base font-semibold text-gray-900">
              To buy <span className="text-gray-500 font-normal">({toBuy.length})</span>
            </h2>
            <button
              type="button"
              onClick={() => setClearConfirmSection('toBuy')}
              className="text-sm text-gray-500 hover:text-red-600"
            >
              Clear all
            </button>
          </div>
          <ul className="divide-y divide-gray-100">
            {toBuy.map((item) => (
              <li key={item.id} className="flex items-center gap-4 p-4 hover:bg-gray-50/50 transition-colors">
                <button
                  type="button"
                  onClick={() => handleMarkPurchased(item.id)}
                  aria-label={`Mark ${item.itemName || 'item'} as purchased`}
                  title="Click when bought – moves to Purchased"
                  className="flex-shrink-0 w-7 h-7 rounded-md border-2 border-gray-300 hover:border-emerald-500 hover:bg-emerald-50 transition-colors flex items-center justify-center"
                />
                <div className="flex-1 min-w-0">
                  <div className="font-medium text-gray-900 truncate">{item.itemName || 'Unknown item'}</div>
                  <div className="text-sm text-gray-500">
                    {formatQuantity(item.quantity)} {item.unit || 'pieces'}
                  </div>
                  {item.addedAt && (
                    <div className="text-xs text-gray-400 mt-0.5">Added {formatDate(item.addedAt)}</div>
                  )}
                </div>
                <button
                  onClick={() => handleDelete(item.id)}
                  aria-label={`Remove ${item.itemName || 'item'}`}
                  className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                >
                  <Trash2 size={18} />
                </button>
              </li>
            ))}
          </ul>
          <div className="px-5 py-3 bg-primary-50/40 border-t border-primary-100/80 text-sm text-slate-600">
            Click the box when you buy an item – it moves to &quot;Purchased&quot; and records the purchase date.
          </div>
        </motion.div>
      )}

      {/* Purchased section */}
      {purchased.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="card overflow-hidden"
        >
          <div className="px-5 py-4 flex items-center justify-between">
            <button
              type="button"
              onClick={() => setShowPurchased(!showPurchased)}
              className="flex-1 flex items-center justify-between text-left hover:bg-gray-50/50 -mx-2 px-2 py-1 rounded-lg transition-colors"
            >
              <h2 className="text-base font-semibold text-gray-900">
                Purchased <span className="text-gray-500 font-normal">({purchased.length})</span>
              </h2>
              {showPurchased ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
            </button>
            <button
              type="button"
              onClick={() => setClearConfirmSection('purchased')}
              className="text-sm text-gray-500 hover:text-red-600 ml-2"
            >
              Clear all
            </button>
          </div>
          {showPurchased && (
            <ul className="divide-y divide-gray-100 border-t border-gray-100">
              {purchased.map((item) => (
                <li key={item.id} className="flex items-center gap-4 p-4 bg-gray-50/30">
                  <div className="flex-shrink-0 w-6 h-6 rounded border-2 border-emerald-500 bg-emerald-500 flex items-center justify-center">
                    <Check className="w-3.5 h-3.5 text-white" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="font-medium text-gray-700 line-through truncate">{item.itemName || 'Unknown item'}</div>
                    <div className="text-sm text-gray-500">{formatQuantity(item.quantity)} {item.unit || 'pieces'}</div>
                    <div className="text-xs text-gray-400 mt-0.5 space-x-3">
                      {item.addedAt && <span>Added {formatDate(item.addedAt)}</span>}
                      {item.purchasedAt && <span>Purchased {formatDate(item.purchasedAt)}</span>}
                    </div>
                  </div>
                  <button
                    onClick={() => setDeleteConfirmItem({ id: item.id, itemName: item.itemName || 'item' })}
                    aria-label={`Remove ${item.itemName || 'item'}`}
                    className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                  >
                    <Trash2 size={18} />
                  </button>
                </li>
              ))}
            </ul>
          )}
        </motion.div>
      )}

      {/* Delete single item confirmation */}
      {deleteConfirmItem && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
          onClick={() => setDeleteConfirmItem(null)}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white rounded-2xl shadow-card w-full max-w-sm p-6"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-lg font-display font-semibold text-slate-900 mb-2">Delete item?</h3>
            <p className="text-slate-600 mb-6">
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
                className="flex-1 px-4 py-2.5 rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-50"
              >
                Cancel
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}

      {/* Clear all confirmation */}
      {clearConfirmSection && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
          onClick={() => setClearConfirmSection(null)}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white rounded-2xl shadow-card w-full max-w-sm p-6"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-lg font-display font-semibold text-slate-900 mb-2">Clear all?</h3>
            <p className="text-slate-600 mb-6">
              Are you sure you want to delete all {clearConfirmSection === 'toBuy' ? 'to-buy' : 'purchased'} items? This cannot be undone.
            </p>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={clearConfirmSection === 'toBuy' ? handleClearUnpurchased : handleClearPurchased}
                className="flex-1 px-4 py-2.5 rounded-xl bg-red-600 text-white font-medium hover:bg-red-700 transition-colors"
              >
                Clear all
              </button>
              <button
                type="button"
                onClick={() => setClearConfirmSection(null)}
                className="flex-1 px-4 py-2.5 rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-50"
              >
                Cancel
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}

      {/* Add from Meal Plan / Add item manually when has items */}
      {toBuy.length > 0 && (
        <div className="flex flex-wrap justify-center gap-3">
          <Link
            to="/meal-plan"
            className="inline-flex items-center gap-2 text-sm text-emerald-600 hover:text-emerald-700 font-medium"
          >
            <Calendar size={16} />
            Add more from Meal Plan
          </Link>
          <button
            type="button"
            onClick={() => setShowAddModal(true)}
            className="inline-flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 font-medium"
          >
            <Plus size={16} />
            Add item manually
          </button>
        </div>
      )}

      {/* Add Item modal */}
      {showAddModal && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
          onClick={() => setShowAddModal(false)}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-white rounded-2xl shadow-card w-full max-w-md p-6"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-display font-semibold text-slate-900">Add item</h3>
              <button
                type="button"
                onClick={() => setShowAddModal(false)}
                className="p-1 rounded-lg text-gray-500 hover:bg-gray-100"
              >
                <X size={20} />
              </button>
            </div>
            <form onSubmit={handleAddItem} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Item name</label>
                <input
                  type="text"
                  value={addName}
                  onChange={(e) => setAddName(e.target.value)}
                  placeholder="e.g. Milk, Rice, Tomatoes"
                  className="w-full px-4 py-2.5 rounded-lg border border-gray-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                  autoFocus
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Quantity</label>
                  <input
                    type="text"
                    value={addQty}
                    onChange={(e) => setAddQty(e.target.value)}
                    placeholder="1"
                    className="w-full px-4 py-2.5 rounded-lg border border-gray-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Unit</label>
                  <select
                    value={addUnit}
                    onChange={(e) => setAddUnit(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-lg border border-gray-300 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                  >
                    {UNITS.map((u) => (
                      <option key={u} value={u}>{u}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="flex gap-3 pt-2">
                <motion.button
                  type="submit"
                  disabled={adding}
                  whileHover={{ scale: 1.01 }}
                  whileTap={{ scale: 0.99 }}
                  className="flex-1 btn-primary disabled:opacity-50"
                >
                  {adding ? 'Adding…' : 'Add'}
                </motion.button>
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2.5 rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-50"
                >
                  Cancel
                </button>
              </div>
            </form>
          </motion.div>
        </motion.div>
      )}
    </div>
  )
}

export default ShoppingList
