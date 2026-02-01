import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="bg-gray-900 text-white mt-16">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* About */}
          <div>
            <h3 className="text-xl font-bold mb-4 text-primary-400">✨ Parfumé</h3>
            <p className="text-gray-400 text-sm">
              Your destination for luxury fragrances. Discover the perfect scent for every occasion.
            </p>
          </div>

          {/* Quick Links */}
          <div>
            <h4 className="font-semibold mb-4">Quick Links</h4>
            <ul className="space-y-2 text-sm text-gray-400">
              <li><Link to="/products" className="hover:text-primary-400">Shop All</Link></li>
              <li><Link to="/products?category=Men" className="hover:text-primary-400">Men's Fragrances</Link></li>
              <li><Link to="/products?category=Women" className="hover:text-primary-400">Women's Fragrances</Link></li>
              <li><Link to="/products?featured=true" className="hover:text-primary-400">Featured</Link></li>
            </ul>
          </div>

          {/* Customer Service */}
          <div>
            <h4 className="font-semibold mb-4">Customer Service</h4>
            <ul className="space-y-2 text-sm text-gray-400">
              <li><a href="#" className="hover:text-primary-400">Contact Us</a></li>
              <li><a href="#" className="hover:text-primary-400">Shipping Info</a></li>
              <li><a href="#" className="hover:text-primary-400">Returns</a></li>
              <li><a href="#" className="hover:text-primary-400">FAQ</a></li>
            </ul>
          </div>

          {/* Newsletter */}
          <div>
            <h4 className="font-semibold mb-4">Newsletter</h4>
            <p className="text-sm text-gray-400 mb-4">
              Subscribe to get special offers and updates.
            </p>
            <div className="flex">
              <input
                type="email"
                placeholder="Your email"
                className="px-4 py-2 rounded-l-lg text-gray-900 flex-grow"
              />
              <button className="bg-primary-600 px-4 py-2 rounded-r-lg hover:bg-primary-700">
                Subscribe
              </button>
            </div>
          </div>
        </div>

        <div className="border-t border-gray-800 mt-8 pt-8 text-center text-sm text-gray-400">
          <p>&copy; 2026 Parfumé. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
}
