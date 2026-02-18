import { useState, useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import axios from '../utils/axios'
import { Calendar as CalendarIcon, ChevronLeft, ChevronRight, ShoppingCart, RotateCcw, Settings, Download, HelpCircle, BookOpen, Plus, X, Printer, Copy, Link2 } from 'lucide-react'
import SlidingTicker from '../components/common/SlidingTicker'
import { format, startOfWeek, addDays, addWeeks, subWeeks, startOfMonth, endOfMonth, eachDayOfInterval, isSameMonth, addMonths, subMonths } from 'date-fns'
import { useNotification } from '../contexts/NotificationContext'
import { refreshNotifications } from '../utils/notifications'
import { getLocalDateStr } from '../utils/date'
import { jsPDF } from 'jspdf'
import 'jspdf-autotable'

/** Dietary rule templates: user picks from dropdown, then sets value */
const DIETARY_RULE_TEMPLATES = [
  { id: 'NO_GLUTEN', label: 'No gluten', valueType: 'boolean', default: false },
  { id: 'NO_DAIRY', label: 'No dairy', valueType: 'boolean', default: false },
  { id: 'MIN_VEGETARIAN_DINNERS', label: 'Min vegetarian dinners per week', valueType: 'number', default: 0, min: 0, max: 7 },
  { id: 'VEGAN_ONLY', label: 'Vegan only', valueType: 'boolean', default: false },
  { id: 'MAX_CALORIES_PER_DINNER', label: 'Max calories per dinner (optional)', valueType: 'number', default: null, min: 0 },
  { id: 'MAX_MEAT_DINNERS', label: 'Max meat dinners per week', valueType: 'number', default: 7, min: 0, max: 7 }
]

/**
 * Meal Plan Page – weekly planning with add recipe, batch/leftover, shopping list, share & dietary rules.
 */
const MealPlan = () => {
  const [currentWeek, setCurrentWeek] = useState(new Date())
  const [mealPlans, setMealPlans] = useState([])
  const [loading, setLoading] = useState(true)
  const [showCalendarPopover, setShowCalendarPopover] = useState(false)
  const [calendarMonth, setCalendarMonth] = useState(new Date())
  const [preferences, setPreferences] = useState({ dietaryRules: {} })
  const [shoppingAdded, setShoppingAdded] = useState(null)
  const [lastRemovedPlan, setLastRemovedPlan] = useState(null)
  const [viewingPlanDetails, setViewingPlanDetails] = useState(null)
  const [showPreferences, setShowPreferences] = useState(false)
  const [showAddModal, setShowAddModal] = useState(false)
  const [addSlot, setAddSlot] = useState(null)
  const [savedRecipes, setSavedRecipes] = useState([])
  const [addAsBatch, setAddAsBatch] = useState(false)
  const [leftoverSuggestions, setLeftoverSuggestions] = useState(null)
  const [exporting, setExporting] = useState(false)
  const [suggestionsForSlot, setSuggestionsForSlot] = useState([])
  const [suggestionsLoading, setSuggestionsLoading] = useState(false)
  const [selectedSuggestCuisine, setSelectedSuggestCuisine] = useState(null)
  const [customRecipeName, setCustomRecipeName] = useState('')
  const [previewSuggestion, setPreviewSuggestion] = useState(null)
  const [previewDetailsLoading, setPreviewDetailsLoading] = useState(false)
  const [showHowItWorks, setShowHowItWorks] = useState(() => {
    try {
      return localStorage.getItem('mealplan-how-it-works-dismissed') !== 'true'
    } catch {
      return true
    }
  })
  const [showAddAgainConfirm, setShowAddAgainConfirm] = useState(false)
  const [addToShopAdding, setAddToShopAdding] = useState(false)
  const popoverRef = useRef(null)
  const printRef = useRef(null)
  const { showSuccess, showError, showInfo } = useNotification()

  const weekStart = startOfWeek(currentWeek, { weekStartsOn: 1 }) // Monday
  const weekDays = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i))
  const mealTypes = ['BREAKFAST', 'LUNCH', 'DINNER']

  useEffect(() => {
    fetchMealPlans()
  }, [currentWeek])

  useEffect(() => {
    const fetchPrefs = async () => {
      try {
        const res = await axios.get('/api/meal-plans/preferences')
        const data = res.data || {}
        setPreferences({ dietaryRules: data.dietaryRules || {} })
      } catch (e) {
        console.error(e)
      }
    }
    fetchPrefs()
  }, [])

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

  const getMealPlansForSlot = (date, mealType) => {
    const dateStr = format(date, 'yyyy-MM-dd')
    return (mealPlans || []).filter((plan) => {
      const planDate = plan.date
      const planDateStr = typeof planDate === 'string'
        ? planDate.slice(0, 10)
        : format(new Date(planDate), 'yyyy-MM-dd')
      return planDateStr === dateStr && plan.mealType === mealType
    })
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

  const handleCalendarIconClick = () => {
    setCalendarMonth(weekStart)
    setShowCalendarPopover((prev) => !prev)
  }

  const handleCalendarDateSelect = (date) => {
    setCurrentWeek(date)
    setShowCalendarPopover(false)
  }

  const calendarMonthStart = startOfMonth(calendarMonth)
  const calendarMonthEnd = endOfMonth(calendarMonth)
  const calendarDays = eachDayOfInterval({ start: calendarMonthStart, end: calendarMonthEnd })

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (popoverRef.current && !popoverRef.current.contains(e.target)) {
        setShowCalendarPopover(false)
      }
    }
    if (showCalendarPopover) {
      document.addEventListener('mousedown', handleClickOutside)
    }
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [showCalendarPopover])

  const weekStartStr = format(weekStart, 'yyyy-MM-dd')
  const endDateStr = format(addDays(weekStart, 6), 'yyyy-MM-dd')

  const savePreferences = async () => {
    try {
      const eff = getEffectiveDietaryRules()
      const normalized = {}
      getActiveRulesForDisplay().forEach((t) => {
        const v = eff[t.id]
        normalized[t.id] = v !== undefined && v !== null ? v : t.default
      })
      await axios.put('/api/meal-plans/preferences', { patterns: [], dietaryRules: normalized })
      setPreferences(prev => ({ ...prev, dietaryRules: normalized }))
      showSuccess('Preferences saved')
      setShowPreferences(false)
    } catch (e) {
      showError('Failed to save preferences')
    }
  }

  const addWeekToShoppingList = async (forceFull = false) => {
    setAddToShopAdding(true)
    try {
      const addedDate = getLocalDateStr()
      const res = await axios.post(`/api/shopping-list/from-week?startDate=${weekStartStr}&endDate=${endDateStr}&addedDate=${addedDate}&forceFull=${forceFull}`)
      const data = res.data || {}
      const count = data.addedCount ?? 0
      const alreadyAdded = data.alreadyAdded === true

      if (count > 0) {
        setShoppingAdded({ count, weekStart: data.weekStart })
        showSuccess(`Added ${count} item(s) to shopping list. You can undo.`)
      } else if (!alreadyAdded) {
        setShoppingAdded(null)
        showInfo('No ingredients to add yet. Add recipes to your meal plan first.')
      }

      setShowAddAgainConfirm(false)
      return { alreadyAdded }
    } catch (e) {
      const data = e.response?.data
      const msg = (data && (data.message || data.error)) || 'Failed to add to shopping list. Add recipes with ingredients to your plan first.'
      showError(msg)
      return null
    } finally {
      setAddToShopAdding(false)
    }
  }

  const handleAddWeekToShoppingListClick = async () => {
    const res = await addWeekToShoppingList(false)
    if (res?.alreadyAdded) {
      setShowAddAgainConfirm(true)
    }
  }

  const handleAddAgainConfirm = () => {
    addWeekToShoppingList(true)
  }

  const undoWeekShoppingList = async () => {
    if (!shoppingAdded?.weekStart) return
    try {
      const res = await axios.post(`/api/shopping-list/undo-from-week?weekStart=${shoppingAdded.weekStart}`)
      showSuccess(`Removed ${res.data.removedCount} item(s) from shopping list`)
      setShoppingAdded(null)
    } catch (e) {
      showError('Failed to undo')
    }
  }

  const copyShareLink = () => {
    const url = `${window.location.origin}${window.location.pathname}?week=${weekStartStr}`
    navigator.clipboard.writeText(url)
    showSuccess('Link copied to clipboard')
  }

  const printWeek = () => {
    setExporting(true)
    setTimeout(() => {
      window.print()
      setExporting(false)
    }, 300)
  }

  const downloadWeekPdf = () => {
    const doc = new jsPDF()
    const pageW = doc.internal.pageSize.getWidth()
    const margin = 20

    doc.setFillColor(241, 245, 249)
    doc.rect(0, 0, pageW, 45, 'F')
    doc.setDrawColor(203, 213, 225)
    doc.setLineWidth(0.5)
    doc.line(0, 45, pageW, 45)

    doc.setFontSize(24)
    doc.setTextColor(15, 23, 42)
    doc.setFont('helvetica', 'bold')
    doc.text('Meal Plan', margin, 22)
    doc.setFontSize(11)
    doc.setFont('helvetica', 'normal')
    doc.setTextColor(71, 85, 105)
    doc.text(`${format(weekStart, 'EEEE, MMMM d')} – ${format(addDays(weekStart, 6), 'EEEE, MMMM d, yyyy')}`, margin, 32)

    const tableData = weekDays.map((day, i) => {
      const bPlans = getMealPlansForSlot(day, 'BREAKFAST')
      const lPlans = getMealPlansForSlot(day, 'LUNCH')
      const dPlans = getMealPlansForSlot(day, 'DINNER')
      const join = (arr) => (arr || []).map((p) => p.recipeName).filter(Boolean).join(' • ') || '—'
      return [
        format(day, 'EEE, MMM d'),
        join(bPlans),
        join(lPlans),
        join(dPlans)
      ]
    })

    doc.autoTable({
      startY: 52,
      head: [['Day', 'Breakfast', 'Lunch', 'Dinner']],
      body: tableData,
      theme: 'striped',
      headStyles: {
        fillColor: [30, 64, 175],
        textColor: 255,
        fontStyle: 'bold',
        halign: 'left',
        fontSize: 11
      },
      alternateRowStyles: { fillColor: [248, 250, 252] },
      columnStyles: {
        0: { cellWidth: 38, fontStyle: 'bold', fillColor: [241, 245, 249] },
        1: { cellWidth: 50 },
        2: { cellWidth: 50 },
        3: { cellWidth: 50 }
      },
      margin: { left: margin, right: margin },
      tableWidth: pageW - margin * 2,
      styles: { fontSize: 10, cellPadding: 4 }
    })

    const finalY = doc.lastAutoTable ? doc.lastAutoTable.finalY + 14 : 60
    doc.setFontSize(9)
    doc.setTextColor(148, 163, 184)
    doc.text('Generated by MealCraft', margin, finalY)
    doc.text(format(new Date(), "MMM d, yyyy 'at' h:mm a"), pageW - margin - 40, finalY, { align: 'right' })

    doc.save(`meal-plan-${weekStartStr}.pdf`)
    showSuccess('PDF downloaded')
  }

  const openAddModal = (date, mealType) => {
    setAddSlot({ date, mealType })
    setShowAddModal(true)
    setAddAsBatch(false)
    setLeftoverSuggestions(null)
    setSuggestionsForSlot([])
    setPreviewSuggestion(null)
    setSelectedSuggestCuisine(null)
    setCustomRecipeName('')
    axios.get('/api/recipes/saved').then(r => setSavedRecipes(r.data || [])).catch(() => setSavedRecipes([]))
    fetchSuggestionsForSlot()
  }

  const addCustomRecipeToSlot = () => {
    const trimmed = customRecipeName?.trim()
    if (!trimmed || !addSlot) return
    const customRecipe = {
      recipeName: trimmed,
      imageUrl: null,
      prepTimeMinutes: null,
      cookTimeMinutes: null,
      servings: null,
      instructions: null,
      ingredients: []
    }
    addRecipeToSlot(customRecipe, addAsBatch)
    setCustomRecipeName('')
  }

  const openSuggestionPreview = async (recipe) => {
    const recipeId = recipe.id || recipe.externalRecipeId
    if (!recipeId) {
      setPreviewSuggestion(recipe)
      return
    }
    setPreviewDetailsLoading(true)
    try {
      const res = await axios.get(`/api/recipes/enhance/${recipeId}`)
      const full = res.data
      // Keep pantry match % from suggestion if enhance didn't return it
      if (full && recipe.matchPercentage != null && (full.matchPercentage == null || full.matchPercentage === undefined)) {
        full.matchPercentage = recipe.matchPercentage
      }
      setPreviewSuggestion(full)
    } catch {
      setPreviewSuggestion(recipe)
    } finally {
      setPreviewDetailsLoading(false)
    }
  }

  const confirmAddSuggestion = () => {
    if (previewSuggestion) {
      addRecipeToSlot(previewSuggestion, addAsBatch)
      setPreviewSuggestion(null)
      setShowAddModal(false)
      setAddSlot(null)
      setSuggestionsForSlot([])
      setSelectedSuggestCuisine(null)
    }
  }

  const fetchSuggestionsForSlot = async (cuisine = null) => {
    setSelectedSuggestCuisine(cuisine)
    setSuggestionsLoading(true)
    try {
      const params = new URLSearchParams()
      if (cuisine) params.set('cuisine', cuisine)
      const dr = getEffectiveDietaryRules()
      const activeRules = {}
      if (dr.NO_GLUTEN === true) activeRules.NO_GLUTEN = true
      if (dr.NO_DAIRY === true) activeRules.NO_DAIRY = true
      if (dr.VEGAN_ONLY === true) activeRules.VEGAN_ONLY = true
      if (Object.keys(activeRules).length > 0) {
        params.set('dietaryRules', JSON.stringify(activeRules))
      }
      const res = await axios.get(`/api/recipes/discover?${params.toString()}`)
      setSuggestionsForSlot(res.data || [])
      if (!(res.data || []).length) showError('No recipes found. Try a different cuisine or add your own recipe.')
    } catch (e) {
      showError('Could not load suggestions')
      setSuggestionsForSlot([])
    } finally {
      setSuggestionsLoading(false)
    }
  }

  const addRecipeToSlot = async (recipe, isBatch = false) => {
    if (!addSlot) return
    try {
      const payload = {
        date: format(addSlot.date, 'yyyy-MM-dd'),
        mealType: addSlot.mealType,
        recipeName: recipe.recipeName,
        imageUrl: recipe.imageUrl,
        prepTimeMinutes: recipe.prepTimeMinutes,
        cookTimeMinutes: recipe.cookTimeMinutes,
        servings: recipe.servings,
        instructions: recipe.instructions,
        ingredients: recipe.ingredients || [],
        savedRecipeId: recipe.id || null,
        isBatch
      }
      const res = await axios.post('/api/meal-plans', payload)
      await fetchMealPlans()
      showSuccess('Recipe added to plan')
      refreshNotifications()
      setLastRemovedPlan(null)
      setShowAddModal(false)
      setAddSlot(null)
      setSuggestionsForSlot([])
      setPreviewSuggestion(null)
      setSelectedSuggestCuisine(null)
      if (isBatch && res.data?.id) {
        const sug = await axios.get(`/api/meal-plans/${res.data.id}/leftover-suggestions`)
        setLeftoverSuggestions({ sourceId: res.data.id, slots: sug.data || [] })
        setShowAddModal(true)
      }
    } catch (e) {
      showError('Failed to add recipe')
    }
  }

  const addLeftoverToSlot = async (sourceMealPlanId, dateStr, mealType) => {
    try {
      await axios.post('/api/meal-plans/leftover', {
        sourceMealPlanId,
        date: typeof dateStr === 'string' ? dateStr : format(dateStr, 'yyyy-MM-dd'),
        mealType
      })
      await fetchMealPlans()
      showSuccess('Leftover added')
      setLeftoverSuggestions(null)
      setShowAddModal(false)
      setAddSlot(null)
      setSelectedSuggestCuisine(null)
    } catch (e) {
      showError(e.response?.data?.message || 'Failed to add leftover')
    }
  }

  const removeMealPlan = async (plan) => {
    const id = typeof plan === 'object' ? plan.id : plan
    const planData = typeof plan === 'object' ? plan : null
    try {
      await axios.delete(`/api/meal-plans/${id}`)
      if (planData) setLastRemovedPlan(planData)
      showSuccess('Removed from plan. You can undo.')
      fetchMealPlans()
    } catch (e) {
      const msg = e.response?.data?.message || e.response?.data?.error || e.message || ''
      if (msg.includes('BATCH_DELETE_LEFTOVERS_FIRST') || msg.includes('leftover')) {
        showError('This is a batch meal with leftovers. First remove the leftover entries from other days (the "Leftover" slots), then you can delete this one.')
      } else {
        showError('Failed to remove')
      }
    }
  }

  const undoRemoveMealPlan = async () => {
    if (!lastRemovedPlan) return
    try {
      const dateStr = lastRemovedPlan.date
        ? (typeof lastRemovedPlan.date === 'string' ? lastRemovedPlan.date.slice(0, 10) : format(lastRemovedPlan.date, 'yyyy-MM-dd'))
        : format(weekStart, 'yyyy-MM-dd')
      await axios.post('/api/meal-plans', {
        date: dateStr,
        mealType: lastRemovedPlan.mealType,
        recipeName: lastRemovedPlan.recipeName,
        imageUrl: lastRemovedPlan.imageUrl,
        prepTimeMinutes: lastRemovedPlan.prepTimeMinutes,
        cookTimeMinutes: lastRemovedPlan.cookTimeMinutes,
        servings: lastRemovedPlan.servings,
        instructions: lastRemovedPlan.instructions,
        ingredients: lastRemovedPlan.ingredients || [],
        savedRecipeId: lastRemovedPlan.savedRecipeId || null,
        isBatch: lastRemovedPlan.isBatch || false
      })
      showSuccess('Recipe restored')
      setLastRemovedPlan(null)
      fetchMealPlans()
    } catch (e) {
      showError('Failed to undo')
    }
  }

  const updateDietaryRule = (ruleId, value) => {
    setPreferences(prev => ({
      ...prev,
      dietaryRules: { ...(prev.dietaryRules || {}), [ruleId]: value }
    }))
  }

  const addDietaryRule = (templateId) => {
    const t = DIETARY_RULE_TEMPLATES.find(x => x.id === templateId)
    if (!t || (preferences.dietaryRules || {})[templateId] !== undefined) return
    updateDietaryRule(templateId, t.default)
  }

  const removeDietaryRule = (ruleId) => {
    const next = { ...(preferences.dietaryRules || {}) }
    delete next[ruleId]
    Object.entries(LEGACY_TO_NEW).forEach(([legacy, id]) => { if (id === ruleId) delete next[legacy] })
    setPreferences(prev => ({ ...prev, dietaryRules: next }))
  }

  const LEGACY_TO_NEW = {
    noGluten: 'NO_GLUTEN', noDairy: 'NO_DAIRY', minVegetarianDinnersPerWeek: 'MIN_VEGETARIAN_DINNERS',
    veganOnly: 'VEGAN_ONLY', maxCaloriesPerDinner: 'MAX_CALORIES_PER_DINNER', maxMeatDinners: 'MAX_MEAT_DINNERS'
  }

  /** Normalize rules for display/storage (use new ids) */
  const getEffectiveDietaryRules = () => {
    const dr = preferences.dietaryRules || {}
    const out = { ...dr }
    Object.entries(LEGACY_TO_NEW).forEach(([legacy, id]) => {
      if (dr[legacy] !== undefined && out[id] === undefined) out[id] = dr[legacy]
    })
    return out
  }

  const isRuleAdded = (templateId) => {
    const dr = preferences.dietaryRules || {}
    if (dr[templateId] !== undefined) return true
    const legacy = Object.entries(LEGACY_TO_NEW).find(([, id]) => id === templateId)?.[0]
    return legacy ? dr[legacy] !== undefined : false
  }

  const getActiveRulesForDisplay = () =>
    DIETARY_RULE_TEMPLATES.filter((t) => isRuleAdded(t.id))

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="w-10 h-10 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <>
    <div className="no-print max-w-7xl mx-auto space-y-6 pb-12">
      {/* Header: title + week nav */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <h1 className="text-2xl sm:text-3xl font-display font-bold text-slate-900 tracking-tight">Meal Plan</h1>
        <div className="flex items-center gap-2">
          <button
            onClick={handlePreviousWeek}
            className="p-2 rounded-lg bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 transition-colors"
            aria-label="Previous week"
          >
            <ChevronLeft size={20} />
          </button>
          <button
            onClick={handleToday}
            className="px-3 py-2 rounded-lg bg-primary-600 text-white text-sm font-medium hover:bg-primary-700 transition-colors"
          >
            Today
          </button>
          <button
            onClick={handleNextWeek}
            className="p-2 rounded-lg bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 transition-colors"
            aria-label="Next week"
          >
            <ChevronRight size={20} />
          </button>
        </div>
      </div>

      {/* Guide – elegant collapsible */}
      {showHowItWorks ? (
        <div className="bg-gradient-to-br from-primary-50 to-blue-50/50 rounded-2xl border border-primary-100 p-6 shadow-soft">
          <div className="flex items-start gap-4">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary-100">
              <BookOpen className="h-5 w-5 text-primary-600" />
            </div>
            <div className="flex-1 min-w-0">
              <h3 className="text-base font-semibold text-gray-900 mb-3">How to plan your week</h3>
              <ol className="space-y-2 text-sm text-gray-700">
                <li className="flex gap-2">
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary-200 text-primary-700 text-xs font-bold">1</span>
                  <span><strong>Click any slot</strong> to add a recipe. Three options: <strong>Suggest from Recipes</strong> (browse by cuisine, pick one, confirm), <strong>Add your own</strong> (type the dish name and confirm), or <strong>Add from saved recipes</strong>. Use &quot;Batch&quot; for cook-once-eat-twice meals. Click a recipe name in a slot to view full details.</span>
                </li>
                <li className="flex gap-2">
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary-200 text-primary-700 text-xs font-bold">2</span>
                  <span><strong>Shopping list</strong> – first add recipes to your slots. Then click &quot;Add this week&apos;s ingredients to Shopping List&quot; to generate a list (pantry is checked automatically). Use &quot;View Shopping List&quot; to see or edit.</span>
                </li>
                <li className="flex gap-2">
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary-200 text-primary-700 text-xs font-bold">3</span>
                  <span><strong>Settings</strong> – add dietary rules (e.g. no gluten, vegan) that apply when suggesting recipes.</span>
                </li>
              </ol>
              <button
                type="button"
                onClick={() => { setShowHowItWorks(false); try { localStorage.setItem('mealplan-how-it-works-dismissed', 'true') } catch {} }}
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
          onClick={() => setShowHowItWorks(true)}
          className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-medium bg-primary-50 text-primary-700 border-2 border-primary-200 hover:bg-primary-100 hover:border-primary-300 transition-all shadow-sm"
        >
          <HelpCircle size={20} />
          View guide – How to plan your week
        </button>
      )}

      {/* Week selector + calendar */}
      <div className="relative" ref={popoverRef}>
        <button
          type="button"
          onClick={handleCalendarIconClick}
          className="w-full sm:w-auto min-w-[240px] bg-white rounded-xl shadow-sm p-3 border border-gray-200 hover:border-primary-300 hover:shadow transition-all flex items-center justify-center gap-2"
          aria-label="Choose week"
        >
          <CalendarIcon className="text-primary-500 flex-shrink-0" size={20} />
          <span className="font-medium text-gray-900">
            {format(weekStart, 'MMM d')} – {format(addDays(weekStart, 6), 'MMM d, yyyy')}
          </span>
        </button>

        {showCalendarPopover && (
          <div className="absolute left-0 sm:left-1/2 sm:-translate-x-1/2 top-full z-50 mt-2 bg-white rounded-xl shadow-xl border border-gray-200 p-4 min-w-[280px]">
            <div className="flex items-center justify-between mb-3">
              <button
                type="button"
                onClick={() => setCalendarMonth((m) => subMonths(m, 1))}
                className="p-1 rounded hover:bg-gray-100 text-gray-700"
                aria-label="Previous month"
              >
                <ChevronLeft size={20} />
              </button>
              <span className="font-semibold text-gray-900">
                {format(calendarMonth, 'MMMM yyyy')}
              </span>
              <button
                type="button"
                onClick={() => setCalendarMonth((m) => addMonths(m, 1))}
                className="p-1 rounded hover:bg-gray-100 text-gray-700"
                aria-label="Next month"
              >
                <ChevronRight size={20} />
              </button>
            </div>
            <div className="grid grid-cols-7 gap-1 text-center">
              {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((d) => (
                <div key={d} className="text-xs font-medium text-gray-500 py-1">
                  {d}
                </div>
              ))}
              {/* Pad start of month to align with Monday */}
              {Array.from({ length: (calendarMonthStart.getDay() + 6) % 7 }, (_, i) => (
                <div key={`pad-${i}`} />
              ))}
              {calendarDays.map((day) => {
                const isCurrentMonth = isSameMonth(day, calendarMonth)
                const isInDisplayedWeek = day >= weekStart && day <= addDays(weekStart, 6)
                return (
                  <button
                    key={day.toISOString()}
                    type="button"
                    onClick={() => handleCalendarDateSelect(day)}
                    className={`
                      py-2 rounded text-sm font-medium transition-colors
                      ${!isCurrentMonth ? 'text-gray-300' : 'text-gray-900 hover:bg-primary-100'}
                      ${isCurrentMonth && isInDisplayedWeek
                        ? 'bg-primary-100 text-primary-800 ring-1 ring-primary-500'
                        : ''}
                    `}
                  >
                    {format(day, 'd')}
                  </button>
                )
              })}
            </div>
            <p className="text-xs text-gray-500 mt-2 text-center">
              Click a day to jump to that week
            </p>
          </div>
        )}
      </div>

      <SlidingTicker variant="compact" />

      {/* Actions */}
      <div className="card overflow-hidden">
        <div className="px-5 py-4 border-b border-gray-100 bg-gray-50/50">
          <div className="flex items-center justify-between gap-4">
            <h2 className="text-base font-semibold text-gray-900">Actions</h2>
            <button
              type="button"
              onClick={() => setShowPreferences(true)}
              className="p-2 rounded-lg text-gray-500 hover:bg-gray-100 hover:text-gray-700 transition-colors"
              title="Dietary rules"
            >
              <Settings size={20} />
            </button>
          </div>
        </div>
        <div className="p-5">
          <div className="flex flex-wrap gap-3">
            {lastRemovedPlan && (
              <div className="inline-flex gap-2">
                <button
                  onClick={undoRemoveMealPlan}
                  className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-amber-500 text-white font-medium hover:bg-amber-600 transition-colors"
                >
                  <RotateCcw size={18} />
                  Undo remove
                </button>
                <button
                  onClick={() => setLastRemovedPlan(null)}
                  className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg border border-gray-300 text-gray-700 font-medium hover:bg-gray-50 transition-colors"
                >
                  <X size={18} />
                  Confirm remove
                </button>
              </div>
            )}
            <div className="flex flex-col gap-0.5">
              <button
                onClick={handleAddWeekToShoppingListClick}
                disabled={addToShopAdding}
                title="Plan meals first, then add their ingredients to your shopping list"
                className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-emerald-600 text-white font-medium hover:bg-emerald-700 disabled:opacity-70 transition-colors self-start"
              >
                <ShoppingCart size={18} />
                Add this week&apos;s ingredients to Shopping List
              </button>
              <span className="text-xs text-gray-500">Plan meals first → ingredients are pulled from recipes and pantry is checked</span>
            </div>
            <Link
              to="/shopping-list"
              title="View and edit your shopping list"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg border border-emerald-300 text-emerald-700 font-medium hover:bg-emerald-50 transition-colors"
            >
              <Link2 size={18} />
              View Shopping List
            </Link>
            {shoppingAdded != null && (
              <div className="inline-flex gap-2">
                <button
                  onClick={undoWeekShoppingList}
                  className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg bg-amber-500 text-white font-medium hover:bg-amber-600 transition-colors"
                >
                  <RotateCcw size={18} />
                  Undo ({shoppingAdded.count} items)
                </button>
                <button
                  onClick={() => setShoppingAdded(null)}
                  className="inline-flex items-center gap-2 px-4 py-2.5 rounded-lg border border-emerald-300 text-emerald-700 font-medium hover:bg-emerald-50 transition-colors"
                >
                  Confirm
                </button>
              </div>
            )}
            <span className="flex-1" />
            <div className="flex items-center gap-2">
              <button
                onClick={copyShareLink}
                className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-gray-300 text-gray-600 text-sm font-medium hover:bg-gray-50 transition-colors"
              >
                <Copy size={16} />
                Copy link
              </button>
              <button
                onClick={downloadWeekPdf}
                className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-primary-300 text-primary-600 text-sm font-medium hover:bg-primary-50 transition-colors"
              >
                <Download size={16} />
                Download PDF
              </button>
              <button
                onClick={printWeek}
                className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-gray-300 text-gray-600 text-sm font-medium hover:bg-gray-50 transition-colors"
              >
                <Printer size={16} />
                Print
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Calendar Grid */}
      <div className="bg-white rounded-xl shadow-sm overflow-hidden border border-gray-200">
        <div className="grid grid-cols-8 border-b border-gray-200 bg-gray-50/80">
          <div className="p-4 font-semibold border-r border-gray-200 text-gray-800 text-sm">Day</div>
          {mealTypes.map((type) => (
            <div key={type} className="p-4 font-semibold border-r border-gray-200 text-center text-gray-800 text-sm last:border-r-0">
              {type}
            </div>
          ))}
        </div>
        {weekDays.map((day) => (
          <div key={day.toISOString()} className="grid grid-cols-8 border-b border-gray-100 last:border-b-0 hover:bg-gray-50/30 transition-colors">
            <div className="p-4 border-r border-gray-100 font-medium text-gray-900">
              <div className="text-sm font-semibold">{format(day, 'EEE')}</div>
              <div className="text-xs text-gray-500 mt-0.5">{format(day, 'MMM d')}</div>
            </div>
            {mealTypes.map((mealType) => {
              const plansInSlot = getMealPlansForSlot(day, mealType)
              return (
                <div
                  key={`${day}-${mealType}`}
                  className="p-2 border-r border-gray-100 min-h-[110px] flex flex-col last:border-r-0"
                >
                  <div className="flex-1 min-h-0 overflow-y-auto space-y-1.5">
                    {plansInSlot.map((plan) => (
                      <div
                        key={plan.id}
                        className="text-xs rounded-lg border border-gray-200 bg-white p-2 shadow-sm hover:shadow transition-shadow"
                      >
                        <button
                          type="button"
                          onClick={() => setViewingPlanDetails(plan)}
                          className="block w-full text-left font-medium text-gray-900 truncate hover:text-primary-600 focus:outline-none focus:ring-0"
                          title="Click to view full name"
                        >
                          {plan.recipeName}
                        </button>
                        {plan.isBatch && (
                          <span className="text-xs text-primary-600 font-medium">Batch</span>
                        )}
                        <div className="flex flex-wrap gap-1 mt-1.5">
                          <button
                            type="button"
                            onClick={() => removeMealPlan(plan)}
                            className="text-xs font-medium text-red-600 hover:text-red-700"
                          >
                            Remove
                          </button>
                          {plan.isBatch && (
                            <button
                              type="button"
                              onClick={async () => {
                                try {
                                  const sug = await axios.get(`/api/meal-plans/${plan.id}/leftover-suggestions`)
                                  setLeftoverSuggestions({ sourceId: plan.id, slots: sug.data || [] })
                                  setShowAddModal(true)
                                  setAddSlot(null)
                                } catch (err) {
                                  showError('Could not load leftover options')
                                }
                              }}
                              className="text-xs font-medium text-primary-600 hover:text-primary-700"
                            >
                              Add leftover
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                  <button
                    type="button"
                    onClick={() => openAddModal(day, mealType)}
                    className="mt-1.5 w-full py-2 rounded-lg border-2 border-dashed border-gray-200 hover:border-primary-300 hover:bg-primary-50/50 text-gray-500 hover:text-primary-600 font-medium text-xs transition-all flex items-center justify-center gap-1 flex-shrink-0"
                  >
                    <Plus size={14} />
                    Add recipe
                  </button>
                </div>
              )
            })}
          </div>
        ))}
      </div>

      {/* Preferences modal (dietary rules) */}
      {showPreferences && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 max-h-[90vh] overflow-y-auto">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">Dietary rules</h3>
            <p className="text-sm text-gray-500 mb-4">Add rules from the dropdown. These rules are applied when suggesting recipes.</p>
            <div className="mb-4">
              <label className="block text-xs font-medium text-gray-500 uppercase tracking-wider mb-2">Add a rule</label>
              <select
                onChange={(e) => {
                  const v = e.target.value
                  if (v) { addDietaryRule(v); e.target.value = '' }
                }}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-gray-900 text-sm"
              >
                <option value="">Choose a rule to add…</option>
                {DIETARY_RULE_TEMPLATES.filter((t) => !isRuleAdded(t.id)).map((t) => (
                  <option key={t.id} value={t.id}>{t.label}</option>
                ))}
                {getActiveRulesForDisplay().length >= DIETARY_RULE_TEMPLATES.length && (
                  <option value="" disabled>All rules added</option>
                )}
              </select>
            </div>
            <div className="space-y-3">
              {getActiveRulesForDisplay().map((t) => {
                const effective = getEffectiveDietaryRules()
                const val = effective[t.id]
                return (
                  <div key={t.id} className="flex flex-wrap items-center gap-2 p-3 rounded-lg bg-gray-50 border border-gray-100">
                    <span className="flex-1 min-w-[140px] text-sm font-medium text-gray-900">{t.label}</span>
                    {t.valueType === 'boolean' ? (
                      <label className="flex items-center gap-2">
                        <input
                          type="checkbox"
                          checked={!!val}
                          onChange={(e) => updateDietaryRule(t.id, e.target.checked)}
                          className="rounded border-gray-300"
                        />
                        <span className="text-xs text-gray-600">{val ? 'On' : 'Off'}</span>
                      </label>
                    ) : (
                      <input
                        type="number"
                        min={t.min ?? 0}
                        max={t.max}
                        placeholder={t.id.includes('CALOR') ? 'e.g. 500' : ''}
                        value={val ?? ''}
                        onChange={(e) => updateDietaryRule(t.id, e.target.value === '' ? null : parseInt(e.target.value, 10) || 0)}
                        className="w-24 border border-gray-200 rounded-lg px-2 py-1.5 text-sm text-gray-900"
                      />
                    )}
                    <button
                      type="button"
                      onClick={() => removeDietaryRule(t.id)}
                      className="p-1.5 rounded text-gray-400 hover:text-red-600 hover:bg-red-50"
                      title="Remove rule"
                    >
                      <X size={16} />
                    </button>
                  </div>
                )
              })}
            </div>
            {getActiveRulesForDisplay().length === 0 && (
              <p className="text-sm text-gray-500 py-2">No rules added yet. Select one from the dropdown above.</p>
            )}
            <div className="flex flex-wrap gap-2 mt-6">
              <button
                onClick={savePreferences}
                className="flex-1 min-w-[100px] bg-primary-600 text-white py-2 rounded-lg font-medium hover:bg-primary-700"
              >
                Save
              </button>
              <button
                onClick={async () => {
                  const cleared = { patterns: [], dietaryRules: {} }
                  setPreferences({ dietaryRules: {} })
                  try {
                    await axios.put('/api/meal-plans/preferences', cleared)
                    showSuccess('All dietary rules cleared')
                  } catch (e) {
                    showError('Failed to clear dietary rules')
                  }
                }}
                className="px-4 py-2 border border-gray-300 rounded-lg text-gray-600 hover:bg-gray-50 font-medium"
              >
                Clear all
              </button>
              <button
                onClick={() => setShowPreferences(false)}
                className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Meal details popup – full recipe name when clicked from slot */}
      {viewingPlanDetails && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4" onClick={() => setViewingPlanDetails(null)}>
          <div
            className="bg-white rounded-xl shadow-xl max-w-md w-full p-6 border border-gray-100"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1 min-w-0">
                <p className="text-xs font-medium text-primary-600 uppercase tracking-wider mb-1">
                  {viewingPlanDetails.mealType} · {viewingPlanDetails.date ? format(typeof viewingPlanDetails.date === 'string' ? new Date(viewingPlanDetails.date + 'T12:00:00') : viewingPlanDetails.date, 'EEE, MMM d') : ''}
                </p>
                <h3 className="text-xl font-semibold text-gray-900 leading-snug break-words">
                  {viewingPlanDetails.recipeName}
                </h3>
                <div className="flex flex-wrap gap-3 mt-3 text-sm text-gray-500">
                  {viewingPlanDetails.prepTimeMinutes != null && (
                    <span>Prep: {viewingPlanDetails.prepTimeMinutes} min</span>
                  )}
                  {viewingPlanDetails.cookTimeMinutes != null && (
                    <span>Cook: {viewingPlanDetails.cookTimeMinutes} min</span>
                  )}
                  {viewingPlanDetails.servings != null && (
                    <span>{viewingPlanDetails.servings} servings</span>
                  )}
                  {viewingPlanDetails.isBatch && (
                    <span className="text-primary-600 font-medium">Batch / cook once, eat twice</span>
                  )}
                </div>
              </div>
              <button
                type="button"
                onClick={() => setViewingPlanDetails(null)}
                className="p-2 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
                aria-label="Close"
              >
                <X size={20} />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add again confirmation – items from week already on list */}
      {showAddAgainConfirm && (
        <div
          className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/50"
          onClick={() => !addToShopAdding && setShowAddAgainConfirm(false)}
        >
          <div
            className="bg-white rounded-2xl shadow-xl w-full max-w-sm p-6 border border-gray-200"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-lg font-semibold text-gray-900 mb-2">Items already added</h3>
            <p className="text-gray-600 text-sm mb-4">
              Items from this week are already on your shopping list. Do you want to add again? This will replace the current week&apos;s items with a fresh list.
            </p>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={handleAddAgainConfirm}
                disabled={addToShopAdding}
                className="flex-1 px-4 py-2.5 rounded-xl bg-emerald-600 text-white font-medium hover:bg-emerald-700 disabled:opacity-70"
              >
                {addToShopAdding ? 'Adding…' : 'Confirm'}
              </button>
              <button
                type="button"
                onClick={() => setShowAddAgainConfirm(false)}
                disabled={addToShopAdding}
                className="flex-1 px-4 py-2.5 rounded-xl border border-gray-200 text-gray-700 hover:bg-gray-50 font-medium disabled:opacity-70"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add Recipe modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto p-6">
            {leftoverSuggestions ? (
              <>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Add as leftover</h3>
                <p className="text-sm text-gray-600 mb-4">Choose a slot for the leftover meal.</p>
                <div className="space-y-2">
                  {(leftoverSuggestions.slots || []).map((slot, i) => {
                    const slotDate = slot.date ? (typeof slot.date === 'string' ? slot.date : format(slot.date, 'yyyy-MM-dd')) : null
                    const slotDateObj = slotDate ? new Date(slotDate + 'T12:00:00') : new Date()
                    return (
                      <button
                        key={i}
                        onClick={() => addLeftoverToSlot(
                          leftoverSuggestions.sourceId,
                          slotDate,
                          slot.mealType
                        )}
                        className="w-full text-left px-4 py-2 rounded-lg border border-gray-200 hover:bg-primary-50 hover:border-primary-300 font-medium text-gray-900"
                      >
                        {slot.label || `${format(slotDateObj, 'EEE')} ${slot.mealType}`}
                      </button>
                    )
                  })}
                </div>
                <button
                  onClick={() => { setLeftoverSuggestions(null); setShowAddModal(false) }}
                  className="mt-4 text-gray-500 hover:text-gray-700"
                >
                  Skip
                </button>
              </>
            ) : (
              <>
                <h3 className="text-lg font-semibold text-gray-900 mb-1">
                  {addSlot ? `Add recipe – ${format(addSlot.date, 'EEE')} ${addSlot.mealType}` : 'Choose recipe'}
                </h3>
                <p className="text-sm text-gray-500 mb-4">
                  Three ways to add: suggest recipes, enter your own, or pick from saved.
                </p>
                <label className="flex items-center gap-2 mb-4">
                  <input
                    type="checkbox"
                    checked={addAsBatch}
                    onChange={(e) => setAddAsBatch(e.target.checked)}
                    className="rounded border-gray-300"
                  />
                  <span className="text-gray-900 text-sm">Batch / double portion (cook once, eat twice)</span>
                </label>

                {/* 1. Suggest from Recipes – browse by cuisine, click for details, confirm to add */}
                <div className="mb-4">
                  <p className="text-xs font-medium text-gray-600 mb-2">1. Suggest from Recipes</p>
                  {(() => {
                    const dr = getEffectiveDietaryRules()
                    const active = []
                    if (dr.NO_GLUTEN === true) active.push('No gluten')
                    if (dr.NO_DAIRY === true) active.push('No dairy')
                    if (dr.VEGAN_ONLY === true) active.push('Vegan only')
                    return active.length > 0 ? (
                      <p className="text-xs text-primary-600 mb-2">Applying: {active.join(', ')}</p>
                    ) : null
                  })()}
                  <div className="flex flex-wrap gap-1 mb-2">
                    <button
                      type="button"
                      onClick={() => fetchSuggestionsForSlot()}
                      disabled={suggestionsLoading}
                      className={`px-3 py-1.5 rounded-lg text-sm font-medium disabled:opacity-50 ${
                        selectedSuggestCuisine === null
                          ? 'bg-primary-600 text-white hover:bg-primary-700'
                          : 'border border-gray-200 text-gray-700 hover:bg-primary-50 hover:border-primary-300'
                      }`}
                    >
                      All
                    </button>
                    {['Italian', 'Indian', 'Mexican', 'Chinese', 'Mediterranean', 'Japanese'].map((c) => (
                      <button
                        key={c}
                        type="button"
                        onClick={() => fetchSuggestionsForSlot(c)}
                        disabled={suggestionsLoading}
                        className={`px-3 py-1.5 rounded-lg text-sm font-medium disabled:opacity-50 ${
                          selectedSuggestCuisine === c
                            ? 'bg-primary-600 text-white hover:bg-primary-700'
                            : 'border border-gray-200 text-gray-700 hover:bg-primary-50 hover:border-primary-300'
                        }`}
                      >
                        {c}
                      </button>
                    ))}
                  </div>
                  {suggestionsLoading && suggestionsForSlot.length === 0 && (
                    <p className="text-sm text-gray-500 py-2">Loading recipes…</p>
                  )}
                  {suggestionsForSlot.length > 0 && !previewSuggestion && (
                    <div className="mt-2 space-y-2 max-h-48 overflow-y-auto">
                      <p className="text-xs font-medium text-gray-500">Click a dish to see details, then Confirm to add:</p>
                      {suggestionsForSlot.map((r) => (
                        <button
                          key={r.externalRecipeId || r.id || r.recipeName}
                          type="button"
                          onClick={() => openSuggestionPreview(r)}
                          disabled={previewDetailsLoading}
                          className="w-full text-left px-3 py-2 rounded-lg border border-primary-200 bg-primary-50/50 hover:bg-primary-100 flex items-center gap-3 disabled:opacity-70"
                        >
                          {r.imageUrl && (
                            <img src={r.imageUrl} alt="" className="w-10 h-10 object-cover rounded" />
                          )}
                          <span className="font-medium text-gray-900 text-sm">{r.recipeName}</span>
                          {r.matchPercentage != null && (
                            <span className="text-xs text-gray-500 ml-auto">{Math.round(r.matchPercentage)}% match</span>
                          )}
                        </button>
                      ))}
                    </div>
                  )}

                  {/* Preview: full dish details before confirming */}
                  {previewSuggestion && (
                    <div className="mt-3 p-4 rounded-xl border-2 border-primary-300 bg-white shadow-sm space-y-4 max-h-[60vh] overflow-y-auto">
                      <div className="flex items-center justify-between gap-2 pb-2 border-b border-primary-100">
                        <h4 className="text-sm font-semibold text-primary-700">Recipe preview</h4>
                        <button
                          type="button"
                          onClick={() => setPreviewSuggestion(null)}
                          className="text-xs text-gray-500 hover:text-gray-700 font-medium"
                        >
                          ← Back to list
                        </button>
                      </div>
                      {previewDetailsLoading ? (
                        <p className="text-sm text-gray-600">Loading full details…</p>
                      ) : (
                        <>
                          <p className="text-xs text-gray-600">Review ingredients and steps below. If this looks good, confirm to add to your plan.</p>
                          <div className="flex items-start gap-3">
                            {previewSuggestion.imageUrl && (
                              <img
                                src={previewSuggestion.imageUrl}
                                alt=""
                                className="w-24 h-24 object-cover rounded-lg flex-shrink-0"
                              />
                            )}
                            <div className="flex-1 min-w-0">
                              <h4 className="font-semibold text-gray-900">{previewSuggestion.recipeName}</h4>
                              <div className="flex flex-wrap gap-2 mt-1 text-xs text-gray-600">
                                {previewSuggestion.prepTimeMinutes != null && (
                                  <span>Prep: {previewSuggestion.prepTimeMinutes} min</span>
                                )}
                                {previewSuggestion.cookTimeMinutes != null && (
                                  <span>Cook: {previewSuggestion.cookTimeMinutes} min</span>
                                )}
                                {previewSuggestion.servings != null && (
                                  <span>Servings: {previewSuggestion.servings}</span>
                                )}
                                {previewSuggestion.matchPercentage != null && (
                                  <span className="text-primary-600">{Math.round(previewSuggestion.matchPercentage)}% pantry match</span>
                                )}
                              </div>
                            </div>
                          </div>
                          <div>
                            <h5 className="text-sm font-semibold text-gray-900 mb-2">Required ingredients</h5>
                            <ul className="text-sm text-gray-700 space-y-1 list-disc list-inside">
                              {(previewSuggestion.ingredients || []).map((ing, i) => (
                                <li key={i}>
                                  {ing.name}
                                  {(ing.quantity != null || ing.requiredQuantity != null) && (ing.unit || ing.requiredUnit)
                                    ? ` — ${ing.requiredQuantity ?? ing.quantity} ${ing.requiredUnit ?? ing.unit}`
                                    : ''}
                                  {ing.status ? ` (${ing.status})` : ''}
                                </li>
                              ))}
                              {(!previewSuggestion.ingredients || previewSuggestion.ingredients.length === 0) && (
                                <li className="text-gray-500">No ingredients list</li>
                              )}
                            </ul>
                          </div>
                          {previewSuggestion.instructions ? (
                            <div>
                              <h5 className="text-sm font-semibold text-gray-900 mb-2">How to make it</h5>
                              <div className="text-sm text-gray-700 whitespace-pre-line leading-relaxed">
                                {previewSuggestion.instructions}
                              </div>
                            </div>
                          ) : (
                            <p className="text-xs text-gray-500">Full instructions could not be loaded. You can still add this recipe to your plan.</p>
                          )}
                          <div className="flex gap-2 pt-2 border-t border-primary-100">
                            <button
                              type="button"
                              onClick={confirmAddSuggestion}
                              className="flex-1 py-2.5 rounded-lg bg-primary-600 text-white font-medium hover:bg-primary-700"
                            >
                              Confirm & add to plan
                            </button>
                            <button
                              type="button"
                              onClick={() => setPreviewSuggestion(null)}
                              className="px-4 py-2 rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 font-medium"
                            >
                              Back to list
                            </button>
                          </div>
                        </>
                      )}
                    </div>
                  )}
                </div>

                {/* 2. Add your own recipe – type name and confirm */}
                <div className="mb-4">
                  <p className="text-xs font-medium text-gray-600 mb-2">2. Add your own recipe</p>
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={customRecipeName}
                      onChange={(e) => setCustomRecipeName(e.target.value)}
                      placeholder="e.g. Chicken Stir Fry, Oatmeal, etc."
                      className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm text-gray-900 placeholder:text-gray-400 focus:ring-2 focus:ring-primary-500"
                    />
                    <button
                      type="button"
                      onClick={addCustomRecipeToSlot}
                      disabled={!customRecipeName?.trim()}
                      className="px-4 py-2 rounded-lg bg-primary-600 text-white font-medium hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      Confirm
                    </button>
                    <button
                      type="button"
                      onClick={() => setCustomRecipeName('')}
                      className="px-4 py-2 rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50 font-medium"
                    >
                      Clear
                    </button>
                  </div>
                </div>

                {/* 3. Add from saved recipes */}
                <div>
                  <p className="text-xs font-medium text-gray-600 mb-2">3. Add from saved recipes</p>
                  <div className="space-y-2 max-h-48 overflow-y-auto">
                    {savedRecipes.length === 0 ? (
                      <p className="text-gray-500 text-sm py-2">
                        No saved recipes yet. <Link to="/recipes" className="text-primary-600 hover:underline font-medium">Save recipes from Recipes</Link>, or use &quot;Suggest a recipe&quot; above.
                      </p>
                    ) : (
                      savedRecipes.map((r) => (
                        <button
                          key={r.id || r.externalRecipeId}
                          type="button"
                          onClick={() => addRecipeToSlot(r, addAsBatch)}
                          className="w-full text-left px-4 py-3 rounded-lg border border-gray-200 hover:bg-gray-50 flex items-center gap-3"
                        >
                          {r.imageUrl && (
                            <img src={r.imageUrl} alt="" className="w-12 h-12 object-cover rounded" />
                          )}
                          <span className="font-medium text-gray-900">{r.recipeName}</span>
                        </button>
                      ))
                    )}
                  </div>
                </div>

                <div className="mt-4 flex gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setShowAddModal(false)
                      setAddSlot(null)
                      setSuggestionsForSlot([])
                      setPreviewSuggestion(null)
                      setCustomRecipeName('')
                    }}
                    className="px-4 py-2 rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 font-medium"
                  >
                    Cancel
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}

    </div>

      {/* Print-only content – polished layout matching PDF (dark blue header, striped rows) */}
      {exporting && (
        <div ref={printRef} className="hidden print:block print-force-colors p-10 max-w-4xl mx-auto">
          <div className="mb-8 pb-4 border-b-2 border-slate-200" style={{ backgroundColor: 'rgb(241, 245, 249)' }}>
            <h1 className="text-3xl font-bold text-slate-900 tracking-tight">Meal Plan</h1>
            <p className="text-slate-600 mt-2 text-lg">{format(weekStart, 'EEEE, MMMM d')} – {format(addDays(weekStart, 6), 'EEEE, MMMM d, yyyy')}</p>
          </div>
          <table className="w-full text-sm border-collapse shadow-sm rounded-lg overflow-hidden print-force-colors">
            <thead>
              <tr className="text-white" style={{ backgroundColor: 'rgb(30, 64, 175)' }}>
                <th className="text-left p-4 font-semibold">Day</th>
                <th className="text-left p-4 font-semibold">Breakfast</th>
                <th className="text-left p-4 font-semibold">Lunch</th>
                <th className="text-left p-4 font-semibold">Dinner</th>
              </tr>
            </thead>
            <tbody>
              {weekDays.map((day, i) => (
                <tr key={day.toISOString()} className="border-b border-slate-200 print-force-colors" style={i % 2 === 0 ? { backgroundColor: 'rgb(248, 250, 252)' } : { backgroundColor: 'white' }}>
                  <td className="p-4 font-semibold text-slate-900" style={{ backgroundColor: 'rgb(241, 245, 249)' }}>{format(day, 'EEE, MMM d')}</td>
                  {mealTypes.map((mt) => {
                    const plans = getMealPlansForSlot(day, mt)
                    const names = (plans || []).map((p) => p.recipeName).filter(Boolean).join(' • ') || '—'
                    return <td key={mt} className="p-4 text-slate-700">{names}</td>
                  })}
                </tr>
              ))}
            </tbody>
          </table>
          <p className="mt-10 text-xs text-slate-400">Generated by MealCraft</p>
        </div>
      )}
    </>
  )
}

export default MealPlan




