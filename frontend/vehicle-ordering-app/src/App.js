import React, { useState } from 'react';
import './App.css';
import './styles/VehicleOrderForm.css';
import { VehicleOrderForm } from './components/VehicleOrderForm';

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <h1>Cloud-Native Vehicle Ordering System</h1>
        <p>Customize your vehicle and place your order</p>
      </header>
      <main>
        <VehicleOrderForm />
      </main>
    </div>
  );
}

export default App;
