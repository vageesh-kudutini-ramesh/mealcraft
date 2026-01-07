import { useState, useEffect } from 'react'
import axios from '../utils/axios'
import { Calendar as CalendarIcon } from 'lucide-react'
import { format, startOfWeek, addDays, addWeeks, subWeeks } from 'date-fns'

/**
 * Meal Plan Page
 * 
 * Weekly meal planning calendar with drag-and-drop functionality.
 * 
 * @author MealCraft Team
 */
const MealPlan = () => {
  const [currentWeek, setCurrentWeek] = useState(new Date())
  const [mealPlans, setMealPlans] = useState([])
  const [loading, setLoading] = useState(true)

  const weekStart = startOfWeek(currentWeek, { weekStartsOn: 1 }) // Monday
  const weekDays = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i))
  const mealTypes = ['BREAKFAST', 'LUNCH', 'DINNER']

  useEffect(() => {
    fetchMealPlans()
  }, [currentWeek])

  const fetchMealPlans = async () => {
    try {
      const startDate = format(weekStart, 'yyyy-MM-dd')
      const endDate = format(addDays(weekStart, 6), 'yyyy-MM-dd')
      const response = await axios.get(`/api/meal-plans/week?startDate=${startDate}&endDate=${endDate}`)
      setMealPlans(response.data)
    } catch (error) {
      console.error('Error fetching meal plans:', error)
    } finally {
      setLoading(false)
    }
  }

  const getMealPlanForSlot = (date, mealType) => {
    return mealPlans.find(
      (plan) =>
        format(new Date(plan.date), 'yyyy-MM-dd') === format(date, 'yyyy-MM-dd') &&
        plan.mealType === mealType
    )
  }

  const handlePreviousWeek = () => {
    setCurrentWeek(subWeeks(currentWeek, 1))
  }

  const handleNextWeek = () => {
    setCurrentWeek(addWeeks(currentWeek, 1))
  }

  const handleToday = () => {
    setCurrentWeek(new Date())
  }

  if (loading) {
    return <div className="text-center py-12">Loading meal plan...</div>
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Meal Plan</h1>
        <div className="flex items-center space-x-4">
          <button
            onClick={handlePreviousWeek}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
          >
            ← Previous
          </button>
          <button
            onClick={handleToday}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
          >
            Today
          </button>
          <button
            onClick={handleNextWeek}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
          >
            Next →
          </button>
        </div>
      </div>

      {/* Week Display */}
      <div className="bg-white rounded-lg shadow p-4 mb-4">
        <div className="flex items-center justify-center">
          <CalendarIcon className="mr-2 text-primary-600" size={20} />
          <span className="font-semibold">
            {format(weekStart, 'MMM d')} - {format(addDays(weekStart, 6), 'MMM d, yyyy')}
          </span>
        </div>
      </div>

      {/* Calendar Grid */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="grid grid-cols-8 border-b">
          <div className="p-4 font-semibold border-r">Day</div>
          {mealTypes.map((type) => (
            <div key={type} className="p-4 font-semibold border-r text-center">
              {type}
            </div>
          ))}
        </div>
        {weekDays.map((day) => (
          <div key={day.toISOString()} className="grid grid-cols-8 border-b last:border-b-0">
            <div className="p-4 border-r font-medium">
              <div>{format(day, 'EEE')}</div>
              <div className="text-sm text-gray-500">{format(day, 'MMM d')}</div>
            </div>
            {mealTypes.map((mealType) => {
              const mealPlan = getMealPlanForSlot(day, mealType)
              return (
                <div
                  key={`${day}-${mealType}`}
                  className="p-4 border-r min-h-[100px] hover:bg-gray-50 cursor-pointer"
                >
                  {mealPlan ? (
                    <div className="text-sm">
                      <div className="font-medium">{mealPlan.recipeName}</div>
                      <button className="mt-2 text-xs text-primary-600 hover:text-primary-700">
                        View Details
                      </button>
                    </div>
                  ) : (
                    <button className="text-xs text-gray-400 hover:text-gray-600">
                      + Add Recipe
                    </button>
                  )}
                </div>
              )
            })}
          </div>
        ))}
      </div>
    </div>
  )
}

export default MealPlan




