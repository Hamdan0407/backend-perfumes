import { useState, useRef, useEffect } from 'react';
import { toast } from 'react-toastify';
import axios from '../api/axios';

/**
 * Chatbot Component
 * Floating chatbot widget for scent consultation and customer support
 * Maintains conversation state across messages using conversationId
 */

// Global variable to store conversationId across component instances
let globalConversationId = null;

export default function Chatbot() {
  const [isOpen, setIsOpen] = useState(false);
  const [conversationId] = useState(() => {
    // Use global conversationId if already initialized, otherwise create new one
    if (!globalConversationId) {
      globalConversationId = 'conv_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
      console.log('Created new conversation ID:', globalConversationId);
    }
    return globalConversationId;
  });
  const [messages, setMessages] = useState([
    {
      id: 1,
      type: 'bot',
      text: '✨ Welcome to Perfumé! I\'m Sophia, your personal fragrance consultant. 🌸\n\nI\'m here to help you find your perfect scent match! What brings you here today?',
      timestamp: new Date()
    }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  // Scroll to bottom and focus input when new messages arrive or loading completes
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Auto-focus input when bot finishes responding
  useEffect(() => {
    if (!loading && inputRef.current) {
      // Small delay to ensure focus works smoothly
      setTimeout(() => {
        inputRef.current?.focus();
      }, 0);
    }
  }, [loading]);

  /**
   * Send message to chatbot
   * Sends conversationId to maintain state across requests
   */
  const handleSendMessage = async (e) => {
    e.preventDefault();
    
    if (!input.trim()) return;

    // Add user message to chat
    const userMessage = {
      id: messages.length + 1,
      type: 'user',
      text: input,
      timestamp: new Date()
    };

    setMessages([...messages, userMessage]);
    setInput('');
    setLoading(true);

    try {
      // Send to backend with conversationId to maintain state
      const response = await axios.post('/chatbot/chat', {
        message: input,
        conversationId: conversationId  // CRITICAL: Send conversationId to maintain session
      });

      if (response.data.status === 'success') {
        const botMessage = {
          id: messages.length + 2,
          type: 'bot',
          text: response.data.message,
          timestamp: new Date()
        };
        setMessages(prev => [...prev, botMessage]);
      } else {
        toast.error('Failed to get response');
      }
    } catch (error) {
      console.error('Chatbot error:', error);
      const errorMessage = {
        id: messages.length + 2,
        type: 'bot',
        text: 'Sorry, I encountered an error. Please try again or contact support@perfumeshop.com',
        timestamp: new Date()
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Quick suggestion buttons
   */
  const handleQuickSuggestion = async (type) => {
    setLoading(true);
    try {
      const response = await axios.get(`/chatbot/recommend?type=${type}`);
      
      const userMessage = {
        id: messages.length + 1,
        type: 'user',
        text: `I like ${type} fragrances`,
        timestamp: new Date()
      };

      const botMessage = {
        id: messages.length + 2,
        type: 'bot',
        text: response.data.message,
        timestamp: new Date()
      };

      setMessages(prev => [...prev, userMessage, botMessage]);
    } catch (error) {
      toast.error('Failed to get recommendation');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {/* Floating Button */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          style={{
            position: 'fixed',
            bottom: '24px',
            right: '24px',
            width: '64px',
            height: '64px',
            borderRadius: '9999px',
            background: 'linear-gradient(to right, rgb(219, 39, 119), rgb(147, 51, 234))',
            color: 'white',
            border: 'none',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)',
            zIndex: 40,
            transition: 'all 0.3s ease'
          }}
          onMouseEnter={(e) => {
            e.target.style.boxShadow = '0 20px 25px -5px rgba(0, 0, 0, 0.1)';
            e.target.style.transform = 'scale(1.1)';
          }}
          onMouseLeave={(e) => {
            e.target.style.boxShadow = '0 10px 15px -3px rgba(0, 0, 0, 0.1)';
            e.target.style.transform = 'scale(1)';
          }}
          aria-label="Open chatbot"
        >
          <svg
            className="w-8 h-8"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
            />
          </svg>
        </button>
      )}

      {/* Chat Window */}
      {isOpen && (
        <div className="fixed bottom-6 right-6 w-96 h-screen md:h-[600px] bg-white rounded-xl shadow-2xl flex flex-col z-50 border border-gray-200">
          {/* Header */}
          <div className="bg-gradient-to-r from-pink-600 to-purple-600 text-white p-4 rounded-t-xl flex justify-between items-center">
            <div>
              <h3 className="font-bold text-lg">Scent Assistant</h3>
              <p className="text-sm text-pink-100">Online & Ready to Help ✨</p>
            </div>
            <button
              onClick={() => setIsOpen(false)}
              className="text-white hover:bg-white hover:bg-opacity-20 p-2 rounded-lg transition"
            >
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50">
            {messages.map((message) => (
              <div
                key={message.id}
                className={`flex ${message.type === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                <div
                  className={`max-w-xs px-4 py-3 rounded-lg ${
                    message.type === 'user'
                      ? 'bg-pink-600 text-white rounded-br-none'
                      : 'bg-white border border-gray-200 text-gray-800 rounded-bl-none'
                  }`}
                >
                  <p className="text-sm">{message.text}</p>
                  <span className={`text-xs mt-1 block ${
                    message.type === 'user' ? 'text-pink-100' : 'text-gray-400'
                  }`}>
                    {message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </div>
              </div>
            ))}
            {loading && (
              <div className="flex justify-start">
                <div className="bg-white border border-gray-200 text-gray-800 px-4 py-3 rounded-lg rounded-bl-none">
                  <div className="flex space-x-2">
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-100"></div>
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce delay-200"></div>
                  </div>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Quick Suggestions (show only if messages are minimal) */}
          {messages.length <= 1 && !loading && (
            <div className="px-4 py-3 border-t border-gray-200 bg-white">
              <p className="text-xs font-semibold text-gray-600 mb-2">Quick suggestions:</p>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => handleQuickSuggestion('floral')}
                  className="text-xs bg-pink-50 hover:bg-pink-100 text-pink-600 px-3 py-2 rounded-lg transition"
                >
                  🌸 Floral
                </button>
                <button
                  onClick={() => handleQuickSuggestion('woody')}
                  className="text-xs bg-amber-50 hover:bg-amber-100 text-amber-600 px-3 py-2 rounded-lg transition"
                >
                  🌲 Woody
                </button>
                <button
                  onClick={() => handleQuickSuggestion('fresh')}
                  className="text-xs bg-blue-50 hover:bg-blue-100 text-blue-600 px-3 py-2 rounded-lg transition"
                >
                  🌊 Fresh
                </button>
                <button
                  onClick={() => handleQuickSuggestion('oriental')}
                  className="text-xs bg-purple-50 hover:bg-purple-100 text-purple-600 px-3 py-2 rounded-lg transition"
                >
                  ✨ Oriental
                </button>
              </div>
            </div>
          )}

          {/* Input Form */}
          <form onSubmit={handleSendMessage} className="p-4 border-t border-gray-200 bg-white rounded-b-xl">
            <div className="flex gap-2">
              <input
                ref={inputRef}
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="Ask about fragrances..."
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-pink-600"
                disabled={loading}
              />
              <button
                type="submit"
                disabled={loading || !input.trim()}
                className="bg-gradient-to-r from-pink-600 to-purple-600 text-white px-4 py-2 rounded-lg hover:shadow-lg transition disabled:opacity-50"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                </svg>
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
