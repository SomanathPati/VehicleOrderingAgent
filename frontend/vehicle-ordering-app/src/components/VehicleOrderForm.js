import React, { useState } from 'react';
import { KafkaService } from '../services/KafkaService';
import { CloudWatchService } from '../services/CloudWatchService';
import '../styles/VehicleOrderForm.css';

const VEHICLE_MODELS = [
  'Sedan', 'SUV', 'Truck', 'Hatchback', 'Convertible'
];

const COLORS = [
  'Red', 'Blue', 'Black', 'White', 'Silver', 'Green'
];

const WHEELS = [
  'Standard Alloy', 'Premium Alloy', 'Chrome', 'Carbon Fiber'
];

const FEATURES = [
  'Navigation System',
  'Heated Seats',
  'Sunroof',
  'Premium Audio',
  'Leather Interior',
  'Backup Camera'
];

export const VehicleOrderForm = () => {
  const [order, setOrder] = useState({
    customerName: '',
    email: '',
    phone: '',
    model: '',
    color: '',
    wheels: '',
    features: [],
    specialRequests: ''
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitMessage, setSubmitMessage] = useState('');

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setOrder(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleFeatureChange = (feature) => {
    setOrder(prev => ({
      ...prev,
      features: prev.features.includes(feature)
        ? prev.features.filter(f => f !== feature)
        : [...prev.features, feature]
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setSubmitMessage('');

    try {
      // Validate required fields
      if (!order.customerName || !order.email || !order.model || !order.color) {
        throw new Error('Please fill in all required fields');
      }

      // Create order payload
      const orderPayload = {
        orderId: `ORD-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        timestamp: new Date().toISOString(),
        ...order
      };

      // Send to Kafka
      await KafkaService.sendOrder(orderPayload);

      // Log success to CloudWatch
      await CloudWatchService.logEvent('OrderSubmitted', {
        orderId: orderPayload.orderId,
        customerEmail: order.email,
        model: order.model
      });

      setSubmitMessage('Order submitted successfully! Check your email for confirmation.');
      setOrder({
        customerName: '',
        email: '',
        phone: '',
        model: '',
        color: '',
        wheels: '',
        features: [],
        specialRequests: ''
      });

    } catch (error) {
      console.error('Order submission failed:', error);

      // Log error to CloudWatch
      await CloudWatchService.logError('OrderSubmissionFailed', {
        error: error.message,
        customerEmail: order.email
      });

      setSubmitMessage(`Error: ${error.message}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="order-form-container">
      <form onSubmit={handleSubmit} className="order-form">
        <h2>Vehicle Order Form</h2>

        {/* Customer Information */}
        <div className="form-section">
          <h3>Customer Information</h3>
          <div className="form-group">
            <label htmlFor="customerName">Full Name *</label>
            <input
              type="text"
              id="customerName"
              name="customerName"
              value={order.customerName}
              onChange={handleInputChange}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">Email *</label>
            <input
              type="email"
              id="email"
              name="email"
              value={order.email}
              onChange={handleInputChange}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="phone">Phone</label>
            <input
              type="tel"
              id="phone"
              name="phone"
              value={order.phone}
              onChange={handleInputChange}
            />
          </div>
        </div>

        {/* Vehicle Configuration */}
        <div className="form-section">
          <h3>Vehicle Configuration</h3>

          <div className="form-group">
            <label htmlFor="model">Model *</label>
            <select
              id="model"
              name="model"
              value={order.model}
              onChange={handleInputChange}
              required
            >
              <option value="">Select a model</option>
              {VEHICLE_MODELS.map(model => (
                <option key={model} value={model}>{model}</option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="color">Color *</label>
            <select
              id="color"
              name="color"
              value={order.color}
              onChange={handleInputChange}
              required
            >
              <option value="">Select a color</option>
              {COLORS.map(color => (
                <option key={color} value={color}>{color}</option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="wheels">Wheels</label>
            <select
              id="wheels"
              name="wheels"
              value={order.wheels}
              onChange={handleInputChange}
            >
              <option value="">Select wheels</option>
              {WHEELS.map(wheel => (
                <option key={wheel} value={wheel}>{wheel}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Features */}
        <div className="form-section">
          <h3>Additional Features</h3>
          <div className="features-grid">
            {FEATURES.map(feature => (
              <label key={feature} className="feature-checkbox">
                <input
                  type="checkbox"
                  checked={order.features.includes(feature)}
                  onChange={() => handleFeatureChange(feature)}
                />
                {feature}
              </label>
            ))}
          </div>
        </div>

        {/* Special Requests */}
        <div className="form-section">
          <h3>Special Requests</h3>
          <textarea
            name="specialRequests"
            value={order.specialRequests}
            onChange={handleInputChange}
            placeholder="Any special requests or customizations..."
            rows="3"
          />
        </div>

        {/* Submit Button */}
        <button
          type="submit"
          disabled={isSubmitting}
          className="submit-button"
        >
          {isSubmitting ? 'Submitting Order...' : 'Place Order'}
        </button>

        {submitMessage && (
          <div className={`message ${submitMessage.includes('Error') ? 'error' : 'success'}`}>
            {submitMessage}
          </div>
        )}
      </form>
    </div>
  );
};
