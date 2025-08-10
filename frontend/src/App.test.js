import { render } from '@testing-library/react';
import App from './App';

// Mock the components to avoid axios and other dependencies
jest.mock('./components/Auth', () => {
  return function MockAuth() {
    return <div data-testid="auth-component">Auth Component</div>;
  };
});

jest.mock('./components/Dashboard', () => {
  return function MockDashboard() {
    return <div data-testid="dashboard-component">Dashboard Component</div>;
  };
});

jest.mock('./components/Product', () => {
  return function MockProduct() {
    return <div data-testid="product-component">Product Component</div>;
  };
});

jest.mock('./components/Order', () => {
  return function MockOrder() {
    return <div data-testid="order-component">Order Component</div>;
  };
});

jest.mock('./components/History', () => {
  return function MockHistory() {
    return <div data-testid="history-component">History Component</div>;
  };
});

jest.mock('./components/Navigation', () => {
  return function MockNavigation() {
    return <div data-testid="navigation-component">Navigation Component</div>;
  };
});

test('renders app without crashing', () => {
  const { container } = render(<App />);
  // This test just verifies that the app renders without crashing
  expect(container).toBeTruthy();
});
