import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useToast } from '../../context/ToastContext';
import api from '../../services/api';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import StatusBadge from '../../components/common/StatusBadge';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import { Briefcase, Building2, MapPin, Calendar, Clock, AlertTriangle, ExternalLink, XCircle } from 'lucide-react';

export default function CandidateApplicationsPage() {
  const { showToast } = useToast();
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [selectedApp, setSelectedApp] = useState(null);
  const [isWithdrawOpen, setIsWithdrawOpen] = useState(false);
  const [withdrawTargetId, setWithdrawTargetId] = useState(null);

  useEffect(() => {
    fetchApplications();
  }, []);

  const fetchApplications = async () => {
    setLoading(true);
    try {
      const res = await api.get('/applications/my-applications');
      setApplications(res.data || []);
    } catch (error) {
      showToast('Failed to load your applications', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleWithdraw = async () => {
    if (!withdrawTargetId) return;
    try {
      await api.delete(`/applications/${withdrawTargetId}`);
      showToast('Application successfully withdrawn', 'success');
      setApplications(prev => prev.filter(app => app.id !== withdrawTargetId));
    } catch (error) {
      showToast(error.response?.data?.message || 'Failed to withdraw application', 'error');
    } finally {
      setIsWithdrawOpen(false);
      setWithdrawTargetId(null);
    }
  };

  const filtered = applications.filter(app => {
    if (filterStatus === 'ALL') return true;
    return app.status === filterStatus;
  });

  const statuses = ['ALL', 'APPLIED', 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'SELECTED', 'REJECTED'];

  if (loading) {
    return <LoadingSpinner text="Loading your job applications..." />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">My Job Applications</h1>
          <p className="text-sm text-gray-500">
            Track real-time hiring stages, review timelines, and communicate with prospective employers.
          </p>
        </div>
        <Link to="/jobs" className="btn btn-primary self-start sm:self-auto">
          Find More Jobs
        </Link>
      </div>

      {/* Filter Tabs */}
      <div className="flex flex-wrap gap-2 border-b border-gray-200 dark:border-gray-800 pb-3">
        {statuses.map(st => {
          const count = st === 'ALL' ? applications.length : applications.filter(a => a.status === st).length;
          return (
            <button
              key={st}
              onClick={() => setFilterStatus(st)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-semibold transition ${
                filterStatus === st 
                  ? 'bg-primary-600 text-white shadow-sm' 
                  : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300 hover:bg-gray-200'
              }`}
            >
              {st.replace('_', ' ')} ({count})
            </button>
          );
        })}
      </div>

      {/* Applications List */}
      {filtered.length === 0 ? (
        <div className="card p-12 text-center">
          <Briefcase className="mx-auto text-gray-300 dark:text-gray-600 mb-3" size={40} />
          <h3 className="font-bold text-gray-800 dark:text-gray-200">No applications in this status</h3>
          <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
            Try choosing a different status filter or explore open job listings to apply.
          </p>
          <div className="mt-4">
            <Link to="/jobs" className="btn btn-sm btn-primary">
              Browse Openings
            </Link>
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4">
          {filtered.map(app => (
            <div 
              key={app.id} 
              className="card p-5 hover:shadow-md transition border border-gray-200 dark:border-gray-800 flex flex-col md:flex-row justify-between gap-4"
            >
              <div className="space-y-2 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="text-lg font-bold text-gray-900 dark:text-white">
                    {app.jobTitle}
                  </h3>
                  <StatusBadge status={app.status} />
                </div>

                <div className="flex flex-wrap items-center gap-4 text-xs text-gray-500">
                  <span className="flex items-center gap-1">
                    <Building2 size={13} className="text-primary-600" /> {app.companyName || 'Verified Employer'}
                  </span>
                  <span className="flex items-center gap-1">
                    <Calendar size={13} /> Applied on {new Date(app.appliedAt).toLocaleDateString()}
                  </span>
                  {app.updatedAt && (
                    <span className="flex items-center gap-1">
                      <Clock size={13} /> Updated {new Date(app.updatedAt).toLocaleDateString()}
                    </span>
                  )}
                </div>

                {app.coverLetter && (
                  <div className="mt-2 p-3 bg-gray-50 dark:bg-gray-800/40 rounded-lg text-xs text-gray-600 dark:text-gray-300 border border-gray-100 dark:border-gray-800">
                    <span className="font-semibold text-gray-700 dark:text-gray-200">Submitted Note:</span> {app.coverLetter}
                  </div>
                )}

                {/* Progress Visual Tracker */}
                <div className="pt-2">
                  <div className="flex items-center justify-between text-[10px] text-gray-400 font-medium mb-1">
                    <span>Applied</span>
                    <span>Shortlisted</span>
                    <span>Interview</span>
                    <span>Offer / Outcome</span>
                  </div>
                  <div className="w-full bg-gray-200 dark:bg-gray-700 h-2 rounded-full overflow-hidden">
                    <div 
                      className={`h-full rounded-full ${
                        app.status === 'REJECTED' 
                          ? 'bg-rose-500' 
                          : app.status === 'SELECTED' 
                          ? 'bg-emerald-500' 
                          : 'bg-primary-600'
                      }`}
                      style={{
                        width: 
                          app.status === 'APPLIED' ? '25%' :
                          app.status === 'SHORTLISTED' ? '50%' :
                          app.status === 'INTERVIEW_SCHEDULED' ? '75%' :
                          '100%'
                      }}
                    />
                  </div>
                </div>
              </div>

              {/* Action buttons */}
              <div className="flex md:flex-col justify-end items-end gap-2 shrink-0 border-t md:border-t-0 pt-3 md:pt-0">
                <Link to={`/jobs/${app.jobId}`} className="btn btn-xs btn-outline flex items-center gap-1">
                  Job Details <ExternalLink size={12} />
                </Link>
                {app.status === 'APPLIED' && (
                  <button
                    onClick={() => {
                      setWithdrawTargetId(app.id);
                      setIsWithdrawOpen(true);
                    }}
                    className="btn btn-xs btn-danger flex items-center gap-1"
                  >
                    <XCircle size={12} /> Withdraw
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Confirmation Dialog */}
      <ConfirmDialog
        isOpen={isWithdrawOpen}
        title="Withdraw Application"
        message="Are you sure you want to withdraw this application? This action cannot be undone."
        confirmText="Yes, Withdraw"
        cancelText="Keep Application"
        onConfirm={handleWithdraw}
        onCancel={() => {
          setIsWithdrawOpen(false);
          setWithdrawTargetId(null);
        }}
      />
    </div>
  );
}
