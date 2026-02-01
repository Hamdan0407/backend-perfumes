import { Link } from 'react-router-dom';

export default function ProductCard({ product }) {
  const displayPrice = product.discountPrice || product.price;
  const hasDiscount = product.discountPrice && product.discountPrice < product.price;

  return (
    <Link to={`/products/${product.id}`} className="card group">
      <div className="relative overflow-hidden">
        <img
          src={product.imageUrl || 'https://via.placeholder.com/400x500?text=Perfume'}
          alt={product.name}
          className="w-full h-72 object-cover group-hover:scale-110 transition-transform duration-300"
        />
        {hasDiscount && (
          <span className="absolute top-4 right-4 bg-red-500 text-white px-3 py-1 rounded-full text-sm font-semibold">
            Sale
          </span>
        )}
        {product.featured && (
          <span className="absolute top-4 left-4 bg-primary-600 text-white px-3 py-1 rounded-full text-sm font-semibold">
            Featured
          </span>
        )}
      </div>
      
      <div className="p-6">
        <p className="text-sm text-gray-500 mb-1">{product.brand}</p>
        <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
          {product.name}
        </h3>
        
        <div className="flex items-center mb-3">
          <div className="flex items-center">
            {[...Array(5)].map((_, i) => (
              <svg
                key={i}
                className={`w-4 h-4 ${
                  i < Math.floor(product.rating) ? 'text-yellow-400' : 'text-gray-300'
                }`}
                fill="currentColor"
                viewBox="0 0 20 20"
              >
                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
              </svg>
            ))}
            <span className="ml-2 text-sm text-gray-600">({product.reviewCount})</span>
          </div>
        </div>
        
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            {hasDiscount && (
              <span className="text-gray-400 line-through text-sm">
                ₹{product.price.toFixed(2)}
              </span>
            )}
            <span className="text-xl font-bold text-gray-900">
              ₹{displayPrice.toFixed(2)}
            </span>
          </div>
          <span className="text-sm text-gray-500">{product.volume}ml</span>
        </div>
        
        {product.stock < 10 && product.stock > 0 && (
          <p className="text-sm text-orange-600 mt-2">Only {product.stock} left!</p>
        )}
        {product.stock === 0 && (
          <p className="text-sm text-red-600 mt-2">Out of stock</p>
        )}
      </div>
    </Link>
  );
}
