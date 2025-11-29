import React from 'react';
import { Link } from 'react-router-dom';

export const SchemaHomePage: React.FC = () => {
  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6">Schema Application</h1>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Link
          to="/apps/schema/models"
          className="block p-6 bg-white border rounded shadow hover:bg-gray-50"
        >
          <h2 className="text-xl font-semibold mb-2">Model Definition</h2>
          <p className="text-gray-600">Define data models and fields.</p>
        </Link>
        <Link
          to="/apps/schema/records"
          className="block p-6 bg-white border rounded shadow hover:bg-gray-50"
        >
          <h2 className="text-xl font-semibold mb-2">Data Management</h2>
          <p className="text-gray-600">Manage records for defined models.</p>
        </Link>
        <Link
          to="/apps/schema/codes"
          className="block p-6 bg-white border rounded shadow hover:bg-gray-50"
        >
          <h2 className="text-xl font-semibold mb-2">Code Master</h2>
          <p className="text-gray-600">Manage code values for select boxes.</p>
        </Link>
      </div>
    </div>
  );
};
