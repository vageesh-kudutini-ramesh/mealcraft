/**
 * Curated ingredient suggestions by cuisine/region.
 * Used for the Pantry quick-add feature to reduce manual typing.
 * Refresh button shows a different random subset each time for engagement.
 *
 * @author MealCraft Team
 */

/** Fisher-Yates shuffle - returns a new shuffled array */
export function shuffleArray(arr) {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

/** Get a random subset of size `count` from the array. Each call returns different items. */
export function getRandomizedSubset(arr, count = 12) {
  if (!arr || arr.length === 0) return []
  if (arr.length <= count) return shuffleArray(arr)
  return shuffleArray(arr).slice(0, count)
}

/** Number of ingredients to display at a time. Refresh loads a new set. */
export const QUICK_ADD_DISPLAY_COUNT = 12

export const CUISINE_INGREDIENTS = {
  Indian: [
    'Basmati Rice', 'Turmeric', 'Cumin', 'Coriander', 'Garam Masala', 'Red Chili Powder',
    'Mustard Seeds', 'Cardamom', 'Curry Leaves', 'Ghee', 'Chickpea Flour', 'Lentils (Toor Dal)',
    'Poha', 'Paneer', 'Yogurt', 'Coconut Milk', 'Fenugreek', 'Asafoetida', 'Tamarind',
    'Kasuri Methi', 'Bay Leaves', 'Cinnamon Sticks', 'Cloves', 'Black Pepper', 'Saffron',
    'Jaggery', 'Urad Dal', 'Chana Dal', 'Semolina', 'Besan', 'Rice Flour', 'Idli Rice',
    'Kokum', 'Dried Red Chilies', 'Green Chilies', 'Coconut', 'Fresh Mint', 'Cilantro',
    'Mango Powder', 'Black Salt', 'Chaat Masala',
  ],
  Italian: [
    'Olive Oil', 'Parmesan', 'Mozzarella', 'Tomato Paste', 'Balsamic Vinegar', 'Oregano',
    'Basil', 'Garlic', 'Pasta', 'Risotto Rice', 'Capers', 'Anchovies', 'Pine Nuts',
    'Pancetta', 'Marsala Wine', 'Polenta', 'Cannellini Beans', 'Pesto', 'Bread Crumbs',
    'San Marzano Tomatoes', 'Prosciutto', 'Burrata', 'Ricotta', 'Pecorino', 'Fresh Parsley',
    'White Wine', 'Arborio Rice', 'Porcini Mushrooms', 'Sundried Tomatoes', 'Olives',
    'Artichoke Hearts', 'Focaccia', 'Semolina Flour', 'Balsamic Glaze', 'Truffle Oil',
    'Parmigiano Reggiano', 'Fresh Thyme', 'Lemon Zest', 'Red Pepper Flakes',
  ],
  Mexican: [
    'Black Beans', 'Tortillas', 'Chipotle', 'Jalapeño', 'Cilantro', 'Lime', 'Avocado',
    'Cumin', 'Chili Powder', 'Oregano', 'Cotija Cheese', 'Queso Fresco', 'Tomatillos',
    'Salsa', 'Refried Beans', 'Masa Harina', 'Epazote', 'Mexican Chocolate', 'Pickled Jalapeños',
    'Ancho Chilies', 'Guajillo Chilies', 'Chipotle in Adobo', 'Queso Oaxaca', 'Crema',
    'Hominy', 'Poblano Pepper', 'Serrano Pepper', 'Mexican Oregano', 'Achiote Paste',
    'Pozole', 'Horchata Mix', 'Tajin', 'Pumpkin Seeds', 'Dried Corn Husks', 'Piloncillo',
    'Mexican Vanilla', 'Dried Shrimp', 'Cactus Pads (Nopales)',
  ],
  Mediterranean: [
    'Olive Oil', 'Feta Cheese', 'Kalamata Olives', 'Hummus', 'Pita Bread', 'Tahini',
    'Lemon', 'Garlic', 'Oregano', 'Sumac', "Za'atar", 'Pomegranate', 'Couscous',
    'Chickpeas', 'Halloumi', 'Mint', 'Capers', 'Sun-dried Tomatoes', 'Greek Yogurt',
    'Harissa', 'Rose Water', 'Orange Blossom Water', 'Pomegranate Molasses', 'Labneh',
    'Phyllo Dough', 'Bulgur', 'Freekeh', 'Preserved Lemons', 'Haloumi', 'Marinated Artichokes',
    'Red Pepper Paste', 'Anchovy Paste', 'Fresh Dill', 'Flatbread', 'Falafel Mix',
    'Israeli Couscous', 'Barley', 'Aleppo Pepper', 'Urfa Biber',
  ],
  Asian: [
    'Soy Sauce', 'Rice Vinegar', 'Sesame Oil', 'Fish Sauce', 'Rice Noodles', 'Sriracha',
    'Ginger', 'Five-Spice', 'Rice Wine', 'Tofu', 'Hoisin Sauce', 'Oyster Sauce',
    'Sesame Seeds', 'Nori', 'Mirin', 'Miso Paste', 'Rice Paper', 'Coconut Milk', 'Lemongrass',
    'Gochujang', 'Rice Cakes', 'Dashi', 'Kombu', 'Bonito Flakes', 'Tamari', 'Sambal Oelek',
    'Rice Noodles (Fresh)', 'Bean Sprouts', 'Bok Choy', 'Thai Basil', 'Galangal', 'Kaffir Lime Leaves',
    'Shaoxing Wine', 'Black Vinegar', 'Fermented Black Beans', 'Chili Crisp', 'Pickled Ginger',
    'Rice Flour', 'Tapioca Starch', 'Star Anise', 'Sichuan Peppercorns',
  ],
  American: [
    'All-Purpose Flour', 'Baking Soda', 'Vanilla Extract', 'Brown Sugar', 'Maple Syrup',
    'Peanut Butter', 'Cream Cheese', 'Sour Cream', 'Worcestershire Sauce', 'Ketchup',
    'Mustard', 'BBQ Sauce', 'Corn Syrup', 'Shortening', 'Powdered Sugar', 'Cornstarch',
    'Baking Powder', 'Cinnamon', 'Nutmeg', 'Apple Pie Spice', 'Pumpkin Pie Spice',
    'Bisquick', 'Graham Crackers', 'Marshmallows', 'Chocolate Chips', 'Yellow Cake Mix',
    'Evaporated Milk', 'Sweetened Condensed Milk', 'Hamburger Buns', 'Hot Dog Buns',
    'Ritz Crackers', 'Goldfish Crackers', 'Velveeta', 'Liquid Smoke', 'Old Bay Seasoning',
    'Ranch Dressing', 'Buffalo Sauce', 'Sriracha Mayo', 'Pickle Relish',
  ],
}

export const CUISINE_LABELS = Object.keys(CUISINE_INGREDIENTS)
