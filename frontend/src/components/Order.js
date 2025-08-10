import React, { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Alert, Row, Col } from 'react-bootstrap';
import './Auth.css';
import axios from 'axios';

const Order = () => {
  const [orders, setOrders] = useState([]);
  const [products, setProducts] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [loading, setLoading] = useState(true);
  const [formData, setFormData] = useState({
    orderItems: [{ productId: '', quantity: 1 }]
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    fetchOrders();
    fetchProducts();
  }, []);

  const fetchOrders = async () => {
    try {
      setError('');
      const response = await axios.get('/api/orders');
      setOrders(response.data);
    } catch (error) {
      console.error('Error fetching orders:', error);
      setError('Error fetching orders. Please try again.');
    }
  };

  const fetchProducts = async () => {
    try {
      const response = await axios.get('/api/products');
      setProducts(response.data);
    } catch (error) {
      console.error('Error fetching products:', error);
      setError('Error fetching products. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    
    // Validate form data
    const validItems = formData.orderItems.filter(item => item.productId && item.quantity > 0);
    if (validItems.length === 0) {
      setError('Please add at least one product to the order');
      return;
    }
    
    try {
      // Format data according to backend expectations
      const orderData = {
        orderItems: validItems.map(item => {
          const product = products.find(p => p.id == item.productId);
          if (!product) {
            throw new Error(`Product with ID ${item.productId} not found`);
          }
          return {
            product: {
              id: parseInt(item.productId)
            },
            quantity: parseInt(item.quantity),
            unitPrice: parseFloat(product.price)
          };
        })
      };
      
      console.log('Sending order data:', orderData);
      const response = await axios.post('/api/orders', orderData);
      console.log('Order creation response:', response.data);
      setSuccess('Order created successfully');
      fetchOrders();
      setShowModal(false);
      setFormData({ orderItems: [{ productId: '', quantity: 1 }] });
    } catch (error) {
      console.error('Error creating order:', error);
      console.error('Error response:', error.response);
      console.error('Error message:', error.message);
      setError(error.response?.data || 'Error creating order. Please try again.');
    }
  };

  const addOrderItem = () => {
    setFormData({
      ...formData,
      orderItems: [...formData.orderItems, { productId: '', quantity: 1 }]
    });
  };

  const removeOrderItem = (index) => {
    const newOrderItems = formData.orderItems.filter((_, i) => i !== index);
    setFormData({
      ...formData,
      orderItems: newOrderItems
    });
  };

  const updateOrderItem = (index, field, value) => {
    const newOrderItems = formData.orderItems.map((item, i) =>
      i === index ? { ...item, [field]: value } : item
    );
    setFormData({
      ...formData,
      orderItems: newOrderItems
    });
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setFormData({ orderItems: [{ productId: '', quantity: 1 }] });
    setError('');
  };

  if (loading) {
    return (
      <div className="text-center mt-5">
        <div className="spinner-border" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
        <p className="mt-2">Loading orders...</p>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <Row>
          <Col>
            <h2>Orders</h2>
            <p>Manage your orders</p>
          </Col>
          <Col xs="auto">
            <Button variant="primary" onClick={() => setShowModal(true)}>
              Create Order
            </Button>
          </Col>
        </Row>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}
      {success && <Alert variant="success" onClose={() => setSuccess('')} dismissible>{success}</Alert>}

      <Card>
        <Card.Header>
          <h5>Order List</h5>
        </Card.Header>
        <Card.Body>
          {orders.length > 0 ? (
            <Table striped bordered hover>
              <thead>
                <tr>
                  <th>Order Number</th>
                  <th>Date</th>
                  <th>Status</th>
                  <th>Total Amount</th>
                  <th>Items</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id}>
                    <td>{order.orderNumber}</td>
                    <td>{new Date(order.orderDate).toLocaleDateString()}</td>
                    <td>
                      <span className={`badge bg-${order.status === 'PENDING' ? 'warning' : 'success'}`}>
                        {order.status}
                      </span>
                    </td>
                    <td>${order.totalAmount}</td>
                    <td>{order.orderItems?.length || 0} items</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          ) : (
            <p className="text-muted text-center">No orders found. Create your first order!</p>
          )}
        </Card.Body>
      </Card>

      <Modal show={showModal} onHide={handleCloseModal} size="lg">
        <Modal.Header closeButton>
          <Modal.Title>Create New Order</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form onSubmit={handleSubmit}>
            {formData.orderItems.map((item, index) => (
              <Row key={index} className="mb-3">
                <Col md={6}>
                  <Form.Group>
                    <Form.Label>Product</Form.Label>
                    <Form.Select
                      value={item.productId}
                      onChange={(e) => updateOrderItem(index, 'productId', e.target.value)}
                      required
                    >
                      <option value="">Select Product</option>
                      {products.map((product) => (
                        <option key={product.id} value={product.id}>
                          {product.name} - ${product.price} (Stock: {product.quantity})
                        </option>
                      ))}
                    </Form.Select>
                  </Form.Group>
                </Col>
                <Col md={4}>
                  <Form.Group>
                    <Form.Label>Quantity</Form.Label>
                    <Form.Control
                      type="number"
                      min="1"
                      value={item.quantity}
                      onChange={(e) => updateOrderItem(index, 'quantity', e.target.value)}
                      required
                    />
                  </Form.Group>
                </Col>
                <Col md={2}>
                  <Form.Label>&nbsp;</Form.Label>
                  <div>
                    <Button
                      variant="outline-danger"
                      size="sm"
                      onClick={() => removeOrderItem(index)}
                      disabled={formData.orderItems.length === 1}
                    >
                      Remove
                    </Button>
                  </div>
                </Col>
              </Row>
            ))}
            <Button variant="outline-secondary" onClick={addOrderItem}>
              Add Item
            </Button>
          </Form>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handleCloseModal}>
            Cancel
          </Button>
          <Button variant="primary" onClick={handleSubmit}>
            Create Order
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
};

export default Order;
