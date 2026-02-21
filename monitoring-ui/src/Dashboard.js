import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './Dashboard.css';

/**
 * Supply Chain Error Monitoring Dashboard
 * 
 * Displays:
 * 1. Errors that failed to save to MongoDB (DB_SAVE)
 * 2. Errors that failed to send to OSP API (OSP_API)
 * 3. Real-time error statistics
 * 4. Error details and resolution actions
 */
function Dashboard() {
  const [errors, setErrors] = useState([]);
  const [stats, setStats] = useState({
    totalErrors: 0,
    unresolvedErrors: 0,
    resolvedErrors: 0
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
    // Auto-refresh every 30 seconds
    const interval = setInterval(fetchData, 30000);
    return () => clearInterval(interval);
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      // Fetch unresolved errors
      const errorsResponse = await axios.get('/api/errors/unresolved');
      setErrors(errorsResponse.data);

      // Fetch statistics
      const statsResponse = await axios.get('/api/errors/stats');
      setStats(statsResponse.data);
    } catch (error) {
      console.error('Failed to fetch data:', error);
    }
    setLoading(false);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString();
  };

  const getStageColor = (stage) => {
    switch (stage) {
      case 'DB_SAVE':
        return '#e74c3c';
      case 'OSP_API':
        return '#e67e22';
      case 'LOADER_SERVICE':
        return '#f39c12';
      default:
        return '#95a5a6';
    }
  };

  return (
    <div className="dashboard-container">
      {/* Header */}
      <div className="dashboard-header">
        <h1>📊 Supply Chain Error Monitor</h1>
        <p>Real-time monitoring of processing errors</p>
      </div>

      {/* Statistics Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-label">Total Errors</div>
          <div className="stat-value">{stats.totalErrors}</div>
        </div>
        <div className="stat-card error">
          <div className="stat-label">Unresolved Errors</div>
          <div className="stat-value">{stats.unresolvedErrors}</div>
        </div>
        <div className="stat-card success">
          <div className="stat-label">Resolved Errors</div>
          <div className="stat-value">{stats.resolvedErrors}</div>
        </div>
      </div>

      {/* Errors Section */}
      <div className="errors-section">
        <div className="section-header">
          <h2>Unresolved Errors</h2>
          <button className="refresh-button" onClick={fetchData}>
            🔄 Refresh
          </button>
        </div>

        {loading ? (
          <div className="loading">Loading errors...</div>
        ) : errors.length === 0 ? (
          <div className="no-errors">
            <div style={{ fontSize: '4rem', marginBottom: '20px' }}>✅</div>
            <div>No unresolved errors! System is running smoothly.</div>
          </div>
        ) : (
          <div className="error-list">
            {errors.map((error) => (
              <div key={error.id} className="error-card">
                <div className="error-header">
                  <div className="error-type">
                    {error.eventType ? error.eventType.toUpperCase() : 'UNKNOWN'} Event Error
                  </div>
                  <div
                    className="error-stage"
                    style={{ background: getStageColor(error.failureStage) }}
                  >
                    {error.failureStage}
                  </div>
                </div>

                <div className="error-details">
                  <div className="error-detail">
                    <strong>Event ID:</strong> {error.eventId || 'N/A'}
                  </div>
                  <div className="error-detail">
                    <strong>Failed At:</strong> {formatDate(error.failedAt)}
                  </div>
                  <div className="error-detail">
                    <strong>Retry Count:</strong> {error.retryCount || 0}
                  </div>
                </div>

                {error.errorMessage && (
                  <div className="error-message">
                    {error.errorMessage}
                  </div>
                )}

                {error.eventData && (
                  <div style={{ marginTop: '15px' }}>
                    <strong style={{ fontSize: '0.9rem', color: '#666' }}>Event Data:</strong>
                    <pre style={{
                      background: 'white',
                      padding: '10px',
                      borderRadius: '6px',
                      marginTop: '8px',
                      fontSize: '0.75rem',
                      overflow: 'auto',
                      maxHeight: '200px'
                    }}>
                      {JSON.stringify(error.eventData, null, 2)}
                    </pre>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Legend */}
      <div style={{
        marginTop: '30px',
        padding: '20px',
        background: 'white',
        borderRadius: '12px',
        boxShadow: '0 2px 10px rgba(0, 0, 0, 0.05)'
      }}>
        <h3 style={{ marginBottom: '15px', color: '#333' }}>Error Stages:</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '10px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{ width: '20px', height: '20px', background: '#e74c3c', borderRadius: '4px' }}></div>
            <span><strong>DB_SAVE:</strong> Failed to save to MongoDB</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{ width: '20px', height: '20px', background: '#e67e22', borderRadius: '4px' }}></div>
            <span><strong>OSP_API:</strong> Failed to send to external API</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{ width: '20px', height: '20px', background: '#f39c12', borderRadius: '4px' }}></div>
            <span><strong>LOADER_SERVICE:</strong> Failed to route event</span>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
