import React, { useState } from 'react';
import axios from 'axios';
import './ProducerUI.css';

/**
 * Producer UI - Full Event Creation Interface
 * Supports all 4 supply chain event types with complete field coverage.
 */
function ProducerUI() {
  const [eventType, setEventType] = useState('item');
  const [loading, setLoading] = useState(false);
  const [response, setResponse] = useState(null);
  const [submittedType, setSubmittedType] = useState(null);

  const eventTypeLabels = {
    'item': 'Item',
    'trade-item': 'Trade Item',
    'supplier-supply': 'Supplier Supply',
    'shipment': 'Shipment'
  };

  // Item form state
  const [itemData, setItemData] = useState({
    skuId: '',
    itemName: '',
    category: '',
    price: '',
    weight: '',
    dimensions: '',
    status: 'ACTIVE',
    action: 'CREATE'
  });

  // Trade Item form state
  const [tradeItemData, setTradeItemData] = useState({
    gtin: '',
    skuId: '',
    supplierId: '',
    supplierName: '',
    description: '',
    unitOfMeasure: 'EACH',
    minOrderQuantity: '',
    leadTimeDays: '',
    action: 'CREATE'
  });

  // Supplier Supply form state — aligned with backend SupplierSupplyEvent model
  const [supplierSupplyData, setSupplierSupplyData] = useState({
    supplierId: '',
    skuId: '',
    warehouseId: '',
    warehouseName: '',
    availableQuantity: '',
    reservedQuantity: '',
    onOrderQuantity: '',
    reorderPoint: '',
    reorderQuantity: '',
    action: 'CREATE'
  });

  // Shipment form state — aligned with backend ShipmentEvent model
  const [shipmentData, setShipmentData] = useState({
    trackingNumber: '',
    orderId: '',
    carrier: '',
    shipmentStatus: 'CREATED',
    originLocation: '',
    destinationLocation: '',
    currentLocation: '',
    estimatedDeliveryDate: '',
    actualDeliveryDate: '',
    action: 'CREATE'
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setResponse(null);

    let data = {};

    switch (eventType) {
      case 'item':
        data = { ...itemData };
        if (data.price) data.price = parseFloat(data.price);
        if (data.weight) data.weight = parseFloat(data.weight);
        break;
      case 'trade-item':
        data = { ...tradeItemData };
        if (data.minOrderQuantity) data.minOrderQuantity = parseInt(data.minOrderQuantity, 10);
        if (data.leadTimeDays) data.leadTimeDays = parseInt(data.leadTimeDays, 10);
        break;
      case 'supplier-supply':
        data = { ...supplierSupplyData };
        if (data.availableQuantity) data.availableQuantity = parseInt(data.availableQuantity, 10);
        if (data.reservedQuantity) data.reservedQuantity = parseInt(data.reservedQuantity, 10);
        if (data.onOrderQuantity) data.onOrderQuantity = parseInt(data.onOrderQuantity, 10);
        if (data.reorderPoint) data.reorderPoint = parseInt(data.reorderPoint, 10);
        if (data.reorderQuantity) data.reorderQuantity = parseInt(data.reorderQuantity, 10);
        break;
      case 'shipment':
        data = { ...shipmentData };
        break;
      default:
        setLoading(false);
        return;
    }

    data.eventType = eventType;

    // Remove empty string fields so backend defaults apply
    Object.keys(data).forEach(key => {
      if (data[key] === '') delete data[key];
    });

    setSubmittedType(eventType);

    try {
      const headers = { 'Idempotency-Key': `ui-${Date.now()}` };
      const result = await axios.post('/api/events', data, { headers });
      setResponse({ success: true, data: result.data });
    } catch (error) {
      setResponse({
        success: false,
        error: error.response?.data?.error || error.message
      });
    }

    setLoading(false);
  };

  const handleReset = () => {
    switch (eventType) {
      case 'item':
        setItemData({ skuId: '', itemName: '', category: '', price: '', weight: '', dimensions: '', status: 'ACTIVE', action: 'CREATE' });
        break;
      case 'trade-item':
        setTradeItemData({ gtin: '', skuId: '', supplierId: '', supplierName: '', description: '', unitOfMeasure: 'EACH', minOrderQuantity: '', leadTimeDays: '', action: 'CREATE' });
        break;
      case 'supplier-supply':
        setSupplierSupplyData({ supplierId: '', skuId: '', warehouseId: '', warehouseName: '', availableQuantity: '', reservedQuantity: '', onOrderQuantity: '', reorderPoint: '', reorderQuantity: '', action: 'CREATE' });
        break;
      case 'shipment':
        setShipmentData({ trackingNumber: '', orderId: '', carrier: '', shipmentStatus: 'CREATED', originLocation: '', destinationLocation: '', currentLocation: '', estimatedDeliveryDate: '', actualDeliveryDate: '', action: 'CREATE' });
        break;
      default:
        break;
    }
    setResponse(null);
  };

  return (
    <div className="producer-container">
      <div className="producer-header">
        <h1>Supply Chain Event Producer</h1>
        <p>Create and publish events across all supply chain domains</p>
      </div>

      {/* Event Type Selector */}
      <div className="event-type-selector">
        <button className={`event-type-button ${eventType === 'item' ? 'active' : ''}`} onClick={() => setEventType('item')}>
          <span className="event-type-icon">BOX</span>
          <span className="event-type-label">Item</span>
        </button>
        <button className={`event-type-button ${eventType === 'trade-item' ? 'active' : ''}`} onClick={() => setEventType('trade-item')}>
          <span className="event-type-icon">TAG</span>
          <span className="event-type-label">Trade Item</span>
        </button>
        <button className={`event-type-button ${eventType === 'supplier-supply' ? 'active' : ''}`} onClick={() => setEventType('supplier-supply')}>
          <span className="event-type-icon">WHS</span>
          <span className="event-type-label">Supplier Supply</span>
        </button>
        <button className={`event-type-button ${eventType === 'shipment' ? 'active' : ''}`} onClick={() => setEventType('shipment')}>
          <span className="event-type-icon">TRK</span>
          <span className="event-type-label">Shipment</span>
        </button>
      </div>

      <form onSubmit={handleSubmit} className="producer-form">

        {/* ======================== ITEM FORM ======================== */}
        {eventType === 'item' && (
          <>
            <div className="form-section-header">
              <h2>Item Event</h2>
              <p className="form-section-desc">Create or update product items in the catalog</p>
            </div>

            <div className="form-section">
              <h3>Identification</h3>
              <div className="form-row">
                <div className="form-group">
                  <label>SKU ID <span className="required">*</span></label>
                  <input type="text" value={itemData.skuId} onChange={(e) => setItemData({...itemData, skuId: e.target.value})} required placeholder="e.g. SKU-001" />
                </div>
                <div className="form-group">
                  <label>Item Name <span className="required">*</span></label>
                  <input type="text" value={itemData.itemName} onChange={(e) => setItemData({...itemData, itemName: e.target.value})} required placeholder="e.g. Wireless Mouse" />
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Category</label>
                  <input type="text" value={itemData.category} onChange={(e) => setItemData({...itemData, category: e.target.value})} placeholder="e.g. Electronics" />
                </div>
                <div className="form-group">
                  <label>Status</label>
                  <select value={itemData.status} onChange={(e) => setItemData({...itemData, status: e.target.value})}>
                    <option value="ACTIVE">Active</option>
                    <option value="INACTIVE">Inactive</option>
                    <option value="DISCONTINUED">Discontinued</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="form-section">
              <h3>Physical Details</h3>
              <div className="form-row">
                <div className="form-group">
                  <label>Price ($)</label>
                  <input type="number" step="0.01" min="0" value={itemData.price} onChange={(e) => setItemData({...itemData, price: e.target.value})} placeholder="e.g. 29.99" />
                </div>
                <div className="form-group">
                  <label>Weight (kg)</label>
                  <input type="number" step="0.01" min="0" value={itemData.weight} onChange={(e) => setItemData({...itemData, weight: e.target.value})} placeholder="e.g. 0.15" />
                </div>
              </div>
              <div className="form-group">
                <label>Dimensions</label>
                <input type="text" value={itemData.dimensions} onChange={(e) => setItemData({...itemData, dimensions: e.target.value})} placeholder="e.g. 12x8x4 cm" />
              </div>
            </div>

            <div className="form-section">
              <h3>Action</h3>
              <div className="form-group">
                <select value={itemData.action} onChange={(e) => setItemData({...itemData, action: e.target.value})}>
                  <option value="CREATE">CREATE - New item</option>
                  <option value="UPDATE">UPDATE - Modify existing</option>
                  <option value="DELETE">DELETE - Remove item</option>
                </select>
              </div>
            </div>
          </>
        )}

        {/* ======================== TRADE ITEM FORM ======================== */}
        {eventType === 'trade-item' && (
          <>
            <div className="form-section-header">
              <h2>Trade Item Event</h2>
              <p className="form-section-desc">Manage GTIN/supplier associations and procurement parameters</p>
            </div>

            <div className="form-section">
              <h3>Trade Identification</h3>
              <div className="form-row">
                <div className="form-group">
                  <label>GTIN <span className="required">*</span></label>
                  <input type="text" value={tradeItemData.gtin} onChange={(e) => setTradeItemData({...tradeItemData, gtin: e.target.value})} required placeholder="e.g. 12345678901234" maxLength="14" />
                  <span className="field-hint">8, 12, 13, or 14 digit format</span>
                </div>
                <div className="form-group">
                  <label>SKU ID <span className="required">*</span></label>
                  <input type="text" value={tradeItemData.skuId} onChange={(e) => setTradeItemData({...tradeItemData, skuId: e.target.value})} required placeholder="e.g. SKU-001" />
                </div>
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea value={tradeItemData.description} onChange={(e) => setTradeItemData({...tradeItemData, description: e.target.value})} placeholder="Product description for trade catalog" rows="2" />
              </div>
            </div>

            <div className="form-section">
              <h3>Supplier Information</h3>
              <div className="form-row">
                <div className="form-group">
                  <label>Supplier ID</label>
                  <input type="text" value={tradeItemData.supplierId} onChange={(e) => setTradeItemData({...tradeItemData, supplierId: e.target.value})} placeholder="e.g. SUP-001" />
                </div>
                <div className="form-group">
                  <label>Supplier Name</label>
                  <input type="text" value={tradeItemData.supplierName} onChange={(e) => setTradeItemData({...tradeItemData, supplierName: e.target.value})} placeholder="e.g. Acme Supplies" />
                </div>
              </div>
            </div>

            <div className="form-section">
              <h3>Ordering Parameters</h3>
              <div className="form-row">
                <div className="form-group">
                  <label>Unit of Measure</label>
                  <select value={tradeItemData.unitOfMeasure} onChange={(e) => setTradeItemData({...tradeItemData, unitOfMeasure: e.target.value})}>
                    <option value="EACH">Each</option>
                    <option value="CASE">Case</option>
                    <option value="PALLET">Pallet</option>
                    <option value="BOX">Box</option>
                    <option value="KG">Kilogram</option>
                    <option value="LB">Pound</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Min Order Quantity</label>
                  <input type="number" min="1" value={tradeItemData.minOrderQuantity} onChange={(e) => setTradeItemData({...tradeItemData, minOrderQuantity: e.target.value})} placeholder="e.g. 100" />
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Lead Time (days)</label>
                  <input type="number" min="0" value={tradeItemData.leadTimeDays} onChange={(e) => setTradeItemData({...tradeItemData, leadTimeDays: e.target.value})} placeholder="e.g. 14" />
                  <span className="field-hint">Warning if over 30 days</span>
                </div>
                <div className="form-group">
                  <label>Action</label>
                  <select value={tradeItemData.action} onChange={(e) => setTradeItemData({...tradeItemData, action: e.target.value})}>
                    <option value="CREATE">CREATE - New trade item</option>
                    <option value="UPDATE">UPDATE - Modify existing</option>
                    <option value="DELETE">DELETE - Remove</option>
                  </select>
                </div>
              </div>
            </div>
          </>
        )}

        {/* ======================== SUPPLIER SUPPLY FORM ======================== */}
        {eventType === 'supplier-supply' && (
          <>
            <div className="form-section-header">
              <h2>Supplier Supply Event</h2>
              <p className="form-section-desc">Track warehouse inventory levels and reorder thresholds</p>
            </div>

            <div className="form-section">
              <h3>Identification</h3>
              <div className="form-row">
                <div className="form-group">
                  <label>Supplier ID <span className="required">*</span></label>
                  <input type="text" value={supplierSupplyData.supplierId} onChange={(e) => setSupplierSupplyData({...supplierSupplyData, supplierId: e.target.value})} required placeholder="e.g. SUP-001" />
                </div>
                <div className="form-group">
                  <label>SKU ID <span className="required">*</span></label>
                  <input type="text" value={supplierSupplyData.skuId} onChange={(e) => setSupplierSupplyData({...supplierSupplyData, skuId: e.target.value})} required placeholder="e.g. SKU-001" />
                </div>
              </div>
            </div>

            <div className="form-section">
              <h3>Warehouse</h3>
              <div className="form-row">
                <div className="form-group">
                  <label>Warehouse ID <span className="required">*</span></label>
                  <input type="text" value={supplierSupplyData.warehouseId} onChange={(e) => setSupplierSupplyData({...supplierSupplyData, warehouseId: e.target.value})} required placeholder="e.g. WH-EAST-01" />
                  <span className="field-hint">Required for event routing</span>
                </div>
                <div className="form-group">
                  <label>Warehouse Name</label>
                  <input type="text" value={supplierSupplyData.warehouseName} onChange={(e) => setSupplierSupplyData({...supplierSupplyData, warehouseName: e.target.value})} placeholder="e.g. East Coast DC" />
                </div>
              </div>
            </div>

            <div className="form-section">
              <h3>Inventory Levels</h3>
              <div className="form-row">
                <div className="form-group">
                  <label>Available Quantity</label>
                  <input type="number" min="0" value={supplierSupplyData.availableQuantity} onChange={(e) => setSupplierSupplyData({...supplierSupplyData, availableQuantity: e.target.value})} placeholder="e.g. 500" />
                </div>
                <div className="form-group">
                  <label>Reserved Quantity</label>
                  <input type="number" min="0" value={supplierSupplyData.reservedQuantity} onChange={(e) => setSupplierSupplyData({...supplierSupplyData, reservedQuantity: e.target.value})} placeholder="e.g. 50" />
                </div>
              </div>
              <div className="form-group">
                <label>On-Order Quantity</label>
                <input type="number" min="0" value={supplierSupplyData.onOrderQuantity} onChange={(e) => setSupplierSupplyData({...supplierSupplyData, onOrderQuantity: e.target.value})} placeholder="e.g. 200" />
              </div>
            </div>

            <div className="form-section">
              <h3>Reorder Settings</h3>
              <div className="form-row">
                <div className="form-group">
                  <label>Reorder Point</label>
                  <input type="number" min="0" value={supplierSupplyData.reorderPoint} onChange={(e) => setSupplierSupplyData({...supplierSupplyData, reorderPoint: e.target.value})} placeholder="e.g. 100" />
                  <span className="field-hint">Low stock alert triggers below this</span>
                </div>
                <div className="form-group">
                  <label>Reorder Quantity</label>
                  <input type="number" min="0" value={supplierSupplyData.reorderQuantity} onChange={(e) => setSupplierSupplyData({...supplierSupplyData, reorderQuantity: e.target.value})} placeholder="e.g. 500" />
                </div>
              </div>
            </div>

            <div className="form-section">
              <h3>Action</h3>
              <div className="form-group">
                <select value={supplierSupplyData.action} onChange={(e) => setSupplierSupplyData({...supplierSupplyData, action: e.target.value})}>
                  <option value="CREATE">CREATE - New supply record</option>
                  <option value="UPDATE">UPDATE - Update levels</option>
                  <option value="DELETE">DELETE - Remove record</option>
                </select>
              </div>
            </div>
          </>
        )}

        {/* ======================== SHIPMENT FORM ======================== */}
        {eventType === 'shipment' && (
          <>
            <div className="form-section-header">
              <h2>Shipment Event</h2>
              <p className="form-section-desc">Track shipments from origin to destination</p>
            </div>

            <div className="form-section">
              <h3>Shipment Identification</h3>
              <div className="form-row">
                <div className="form-group">
                  <label>Tracking Number <span className="required">*</span></label>
                  <input type="text" value={shipmentData.trackingNumber} onChange={(e) => setShipmentData({...shipmentData, trackingNumber: e.target.value})} required placeholder="e.g. 1Z999AA10123456784" />
                </div>
                <div className="form-group">
                  <label>Order ID</label>
                  <input type="text" value={shipmentData.orderId} onChange={(e) => setShipmentData({...shipmentData, orderId: e.target.value})} placeholder="e.g. ORD-2024-001" />
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Carrier <span className="required">*</span></label>
                  <select value={shipmentData.carrier} onChange={(e) => setShipmentData({...shipmentData, carrier: e.target.value})} required>
                    <option value="">-- Select Carrier --</option>
                    <option value="FedEx">FedEx</option>
                    <option value="UPS">UPS</option>
                    <option value="DHL">DHL</option>
                    <option value="USPS">USPS</option>
                    <option value="Amazon Logistics">Amazon Logistics</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
                <div className="form-group">
                  <label>Shipment Status</label>
                  <select value={shipmentData.shipmentStatus} onChange={(e) => setShipmentData({...shipmentData, shipmentStatus: e.target.value})}>
                    <option value="CREATED">Created</option>
                    <option value="LABEL_CREATED">Label Created</option>
                    <option value="PICKED_UP">Picked Up</option>
                    <option value="IN_TRANSIT">In Transit</option>
                    <option value="OUT_FOR_DELIVERY">Out for Delivery</option>
                    <option value="DELIVERED">Delivered</option>
                    <option value="DELAYED">Delayed</option>
                    <option value="EXCEPTION">Exception</option>
                    <option value="FAILED_DELIVERY">Failed Delivery</option>
                    <option value="RETURNED">Returned</option>
                    <option value="RETURNED_TO_SENDER">Returned to Sender</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="form-section">
              <h3>Locations</h3>
              <div className="form-row">
                <div className="form-group">
                  <label>Origin</label>
                  <input type="text" value={shipmentData.originLocation} onChange={(e) => setShipmentData({...shipmentData, originLocation: e.target.value})} placeholder="e.g. New York, NY" />
                </div>
                <div className="form-group">
                  <label>Destination</label>
                  <input type="text" value={shipmentData.destinationLocation} onChange={(e) => setShipmentData({...shipmentData, destinationLocation: e.target.value})} placeholder="e.g. Los Angeles, CA" />
                </div>
              </div>
              <div className="form-group">
                <label>Current Location</label>
                <input type="text" value={shipmentData.currentLocation} onChange={(e) => setShipmentData({...shipmentData, currentLocation: e.target.value})} placeholder="e.g. Memphis, TN (Hub)" />
              </div>
            </div>

            <div className="form-section">
              <h3>Delivery Dates</h3>
              <div className="form-row">
                <div className="form-group">
                  <label>Estimated Delivery</label>
                  <input type="datetime-local" value={shipmentData.estimatedDeliveryDate} onChange={(e) => setShipmentData({...shipmentData, estimatedDeliveryDate: e.target.value})} />
                </div>
                <div className="form-group">
                  <label>Actual Delivery</label>
                  <input type="datetime-local" value={shipmentData.actualDeliveryDate} onChange={(e) => setShipmentData({...shipmentData, actualDeliveryDate: e.target.value})} />
                </div>
              </div>
            </div>

            <div className="form-section">
              <h3>Action</h3>
              <div className="form-group">
                <select value={shipmentData.action} onChange={(e) => setShipmentData({...shipmentData, action: e.target.value})}>
                  <option value="CREATE">CREATE - New shipment</option>
                  <option value="UPDATE">UPDATE - Status update</option>
                  <option value="DELETE">DELETE - Cancel shipment</option>
                </select>
              </div>
            </div>
          </>
        )}

        {/* Form Actions */}
        <div className="form-actions">
          <button type="button" className="reset-button" onClick={handleReset}>
            Clear Form
          </button>
          <button type="submit" className="submit-button" disabled={loading}>
            {loading ? `Publishing ${eventTypeLabels[eventType]}...` : `Publish ${eventTypeLabels[eventType]}`}
          </button>
        </div>
      </form>

      {/* Response Display — only shown for the event type that was submitted */}
      {response && submittedType === eventType && (
        <div className={`response-message ${response.success ? 'success' : 'error'}`}>
          {response.success ? (
            <div>
              <div className="response-title">{eventTypeLabels[submittedType]} Published Successfully</div>
              <div className="response-details">
                <div><strong>Event ID:</strong> {response.data.eventId}</div>
                <div><strong>Topic:</strong> {response.data.topic}</div>
                <div><strong>Partition:</strong> {response.data.partition}</div>
                <div><strong>Offset:</strong> {response.data.offset}</div>
                {response.data.duplicate && <div className="duplicate-warning">Duplicate {eventTypeLabels[submittedType].toLowerCase()} detected - not re-published</div>}
              </div>
            </div>
          ) : (
            <div>
              <div className="response-title">Failed to Publish {eventTypeLabels[submittedType]}</div>
              <div className="response-details">{response.error}</div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default ProducerUI;
