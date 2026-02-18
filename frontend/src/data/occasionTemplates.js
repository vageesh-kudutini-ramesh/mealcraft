/**
 * Occasion templates – curated items for parties, trips, picnics, etc.
 * Each item can be saved to recipes and has a YouTube search link.
 *
 * @author MealCraft Team
 */

export function shuffleArray(arr) {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

export const OCCASION_DISPLAY_COUNT = 12

export const OCCASION_TEMPLATES = {
  'Throwing a Party': {
    icon: '🎉',
    items: [
      'Birthday Cake', 'Chocolate Brownies', 'Cupcakes', 'Pizza Margherita', 'Nachos with Cheese',
      'Bruschetta', 'Guacamole', 'Mojito', 'Sangria', 'Fruit Punch', 'Cocktail Meatballs',
      'Cheese Board', 'Caprese Skewers', 'Hummus with Pita', 'Spinach Dip', 'Buffalo Wings',
      'Veggie Platter', 'Garlic Bread', 'Quesadillas', 'Spring Rolls', 'Mac and Cheese',
      'Pasta Salad', 'Coleslaw', 'Deviled Eggs', 'Stuffed Mushrooms', 'Bacon Wrapped Dates',
    ],
  },
  'Trekking & Hiking': {
    icon: '🥾',
    items: [
      'Trail Mix', 'Energy Bars', 'Granola', 'Peanut Butter Sandwiches', 'Oatmeal Cookies',
      'Dried Fruits', 'Beef Jerky', 'Fruit Leather', 'Nuts and Seeds', 'Banana Chips',
      'Hard Boiled Eggs', 'Cheese Sticks', 'Carrot Sticks', 'Apple Slices', 'Protein Bars',
      'Nut Butter Packets', 'Crackers with Cheese', 'Hummus Pack', 'Turkey Wraps',
      'Rice Cakes', 'Dark Chocolate', 'Dehydrated Meals', 'Instant Coffee', 'Electrolyte Drink',
    ],
  },
  'Road Trip': {
    icon: '🚗',
    items: [
      'Sandwich Wraps', 'Chips and Salsa', 'Mixed Nuts', 'Pretzels', 'Popcorn',
      'Cheese Crackers', 'Fruit Salad', 'Veggie Sticks', 'Muffins', 'Banana Bread',
      'Granola Bars', 'String Cheese', 'Yogurt Tubes', 'Apple Slices', 'Peanut Butter Crackers',
      'Beef Jerky', 'Rice Krispies', 'Cookies', 'Juice Boxes', 'Bottled Water',
      'Trail Mix', 'Hard Candy', 'Gum', 'Dried Apricots', 'Pita Chips',
    ],
  },
  'Picnic': {
    icon: '🧺',
    items: [
      'Quiche Lorraine', 'Pasta Salad', 'Coleslaw', 'Deviled Eggs', 'Cheese and Crackers',
      'Fruit Salad', 'Sandwich Wraps', 'Hummus and Veggies', 'Caprese Salad', 'Bruschetta',
      'Lemonade', 'Iced Tea', 'Watermelon Slices', 'Berries', 'Brownies', 'Cookies',
      'Fried Chicken', 'Potato Salad', 'Green Salad', 'Carrot Sticks', 'Grapes',
      'Cucumber Sandwiches', 'Turkey Roll-Ups', 'Gazpacho',
    ],
  },
  'Movie Night': {
    icon: '🍿',
    items: [
      'Popcorn', 'Nachos with Cheese', 'Pizza Slices', 'Hot Dogs', 'Chicken Wings',
      'Mozzarella Sticks', 'Buffalo Cauliflower', 'Chocolate Chip Cookies', 'Brownies',
      'Ice Cream Sundae', 'Candy', 'Pretzels', 'Chips and Dip', 'Cheese Board',
      'Guacamole and Chips', 'Sliders', 'Onion Rings', 'French Fries', "S'mores",
      'Milkshake', 'Soda', 'Nachos', 'Quesadillas', 'Garlic Bread', 'Mozzarella Bites',
    ],
  },
  'Game Day': {
    icon: '🏈',
    items: [
      'Nachos Supreme', 'Buffalo Wings', 'Sliders', 'Pizza', 'Chili', 'Guacamole',
      'Chips and Salsa', 'Cheese Dip', 'Jalapeño Poppers', 'Pigs in a Blanket',
      'Meatballs', 'Mac and Cheese', 'Loaded Potato Skins', 'Soft Pretzels',
      'Beer Can Chicken', 'BBQ Ribs', 'Burgers', 'Hot Dogs', 'Onion Rings',
      'French Fries', 'Coleslaw', 'Brownies', 'Beer', 'Soda', 'Dip Trio',
    ],
  },
  'Weekend Brunch': {
    icon: '🍳',
    items: [
      'Eggs Benedict', 'Pancakes', 'French Toast', 'Avocado Toast', 'Shakshuka',
      'Omelette', 'Eggs Florentine', 'Belgian Waffles', 'Croissant', 'Quiche',
      'Breakfast Burrito', 'Granola Parfait', 'Smoothie Bowl', 'Mimosas',
      'Bloody Mary', 'Coffee', 'Fresh Juice', 'Fruit Salad', 'Hash Browns',
      'Bacon', 'Sausage', 'Smoked Salmon', 'Bagel and Cream Cheese', 'Cinnamon Rolls',
    ],
  },
  'Date Night Dinner': {
    icon: '🕯️',
    items: [
      'Beef Wellington', 'Grilled Salmon', 'Chicken Piccata', 'Lamb Chops', 'Risotto',
      'Steak', 'Shrimp Scampi', 'Coq au Vin', 'Paella', 'Pasta Carbonara',
      'Beef Bourguignon', 'Mushroom Risotto', 'Caprese Salad', 'Creme Brulee',
      'Chocolate Lava Cake', 'Tiramisu', 'Wine', 'Garlic Bread', 'Caesar Salad',
      'Asparagus', 'Mashed Potatoes', 'Roasted Vegetables', 'Truffle Fries',
    ],
  },
}

export const OCCASION_LABELS = Object.keys(OCCASION_TEMPLATES)
