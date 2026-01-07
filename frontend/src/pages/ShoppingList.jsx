import { useEffect, useState } from 'react'
import axios from '../utils/axios'
import { Plus, Check, Trash2 } from 'lucide-react'
import { useNotification } from '../contexts/NotificationContext'

/**
 * Shopping List Page
 * 
 * Shopping list management with auto-generation from meal plans.
 * 
 * @author MealCraft Team
 */
const ShoppingList = () => {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const { showSuccess, showError } = useNotification()

  useEffect(() => {
    fetchShoppingList()
  }, [])

  const fetchShoppingList = async () => {
    try {
      const response = await axios.get('/api/shopping-list/unpurchased')
      setItems(response.data)
    } catch (error) {
      showError('Error loading shopping list')
    } finally {
      setLoading(false)
    }
  }

  const handleMarkPurchased = async (id) => {
    try {
      await axios.post(`/api/shopping-list/${id}/purchase`)
      showSuccess('Item marked as purchased')
      fetchShoppingList()
    } catch (error) {
      showError('Error updating item')
    }
  }

  const handleDelete = async (id) => {
    try {
      await axios.delete(`/api/shopping-list/${id}`)
      showSuccess('Item deleted')
      fetchShoppingList()
    } catch (error) {
      showError('Error deleting item')
    }
  }

  if (loading) {
    return <div className="text-center py-12">Loading shopping list...</div>
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Shopping List</h1>
        <button className="bg-primary-600 text-white px-4 py-2 rounded-lg hover:bg-primary-700 flex items-center">
          <Plus size={20} className="mr-2" />
          Add Item
        </button>
      </div>

      {items.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          Your shopping list is empty. Generate from meal plan or add items manually.
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow">
          <ul className="divide-y">
            {items.map((item) => (
              <li key={item.id} className="p-4 flex items-center justify-between">
                <div className="flex items-center space-x-4">
                  <input
                    type="checkbox"
                    checked={item.isPurchased}
                    onChange={() => handleMarkPurchased(item.id)}
                    className="w-5 h-5 text-primary-600"
                  />
                  <div>
                    <div className="font-medium">{item.itemName}</div>
                    <div className="text-sm text-gray-500">
                      {item.quantity} {item.unit}
                    </div>
                  </div>
                </div>
                <button
                  onClick={() => handleDelete(item.id)}
                  className="text-red-600 hover:text-red-700"
                >
                  <Trash2 size={20} />
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

export default ShoppingList




