import React, { useState, useEffect } from 'react';
import { Card, Table, Form, Row, Col, Alert } from 'react-bootstrap';
import axios from 'axios';

const History = () => {
  const [history, setHistory] = useState([]);
  const [products, setProducts] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState('');
  const [selectedAction, setSelectedAction] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchHistory();
    fetchProducts();
  }, []);

  const fetchHistory = async (productId = '', action = '') => {
    try {
      setError('');
      let url = '/api/history';
      
      if (productId) {
        url = `/api/history/product/${productId}`;
      } else if (action) {
        url = `/api/history/action/${action}`;
      }

      const response = await axios.get(url);
      setHistory(response.data);
    } catch (error) {
      console.error('Error fetching history:', error);
      setError('Error fetching history. Please try again.');
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

  const handleProductChange = (e) => {
    const productId = e.target.value;
    setSelectedProduct(productId);
    if (productId) {
      fetchHistory(productId, '');
      setSelectedAction('');
    } else {
      fetchHistory();
    }
  };

  const handleActionChange = (e) => {
    const action = e.target.value;
    setSelectedAction(action);
    if (action) {
      fetchHistory('', action);
      setSelectedProduct('');
    } else {
      fetchHistory();
    }
  };

  const getActionBadgeColor = (action) => {
    switch (action) {
      case 'ADD': return 'success';
      case 'UPDATE': return 'primary';
      case 'DELETE': return 'danger';
      case 'ORDER': return 'warning';
      default: return 'secondary';
    }
  };

  if (loading) {
    return (
      <div className="text-center mt-5">
        <div className="spinner-border" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
        <p className="mt-2">Loading history...</p>
      </div>
    );
  }

  return (
    <div>
      <div className="page-header">
        <h2>Inventory History</h2>
        <p>Track all inventory changes and activities</p>
      </div>

      {error && <Alert variant="danger" onClose={() => setError('')} dismissible>{error}</Alert>}

      <Card className="mb-4">
        <Card.Header>
          <h5>Filters</h5>
        </Card.Header>
        <Card.Body>
          <Row>
            <Col md={6}>
              <Form.Group>
                <Form.Label>Filter by Product</Form.Label>
                <Form.Select value={selectedProduct} onChange={handleProductChange}>
                  <option value="">All Products</option>
                  {products.map((product) => (
                    <option key={product.id} value={product.id}>
                      {product.name} ({product.sku})
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Col>
            <Col md={6}>
              <Form.Group>
                <Form.Label>Filter by Action</Form.Label>
                <Form.Select value={selectedAction} onChange={handleActionChange}>
                  <option value="">All Actions</option>
                  <option value="ADD">Add Product</option>
                  <option value="UPDATE">Update Product</option>
                  <option value="DELETE">Delete Product</option>
                  <option value="ORDER">Order Placed</option>
                </Form.Select>
              </Form.Group>
            </Col>
          </Row>
        </Card.Body>
      </Card>

      <Card>
        <Card.Header>
          <h5>History Log</h5>
        </Card.Header>
        <Card.Body>
          {history.length > 0 ? (
            <Table striped bordered hover>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Product</th>
                  <th>Action</th>
                  <th>Previous Quantity</th>
                  <th>New Quantity</th>
                  <th>Description</th>
                  <th>User</th>
                </tr>
              </thead>
              <tbody>
                {history.map((entry) => (
                  <tr key={entry.id}>
                    <td>{new Date(entry.createdAt).toLocaleString()}</td>
                    <td>{entry.product?.name || 'N/A'}</td>
                    <td>
                      <span className={`badge bg-${getActionBadgeColor(entry.action)}`}>
                        {entry.action}
                      </span>
                    </td>
                    <td>{entry.previousQuantity || 'N/A'}</td>
                    <td>{entry.newQuantity || 'N/A'}</td>
                    <td>{entry.description}</td>
                    <td>{entry.user?.username || 'N/A'}</td>
                  </tr>
                ))}
              </tbody>
            </Table>
          ) : (
            <div className="text-center text-muted py-4">
              No history records found
            </div>
          )}
        </Card.Body>
      </Card>
    </div>
  );
};

export default History;
