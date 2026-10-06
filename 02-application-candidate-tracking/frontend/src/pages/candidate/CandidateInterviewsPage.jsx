import React, { useState, useEffect } from 'react';
import { useToast } from '../../context/ToastContext';
import api from '../../services/api';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import StatusBadge from '../../components/common/StatusBadge';
import Modal from '../../components/common/Modal';
import { Calendar, Clock, Video, Building2, MapPin, ExternalLink, CheckCircle, FileText, Eye } from 'lucide-react';

export default function CandidateInterviewsPage() {
  const { showToast } = useToast();
  const [interviews, setInterviews] = useState([]);
  const [loading, setLoading] = useState(true);

  // View Details Modal
  const [isViewOpen, setIsViewOpen] = useState(false);
  const [viewTargetInterview, setViewTargetInterview] = useState(null);

  useEffect(() => {
    fetchInterviews();
  }, []);

  const fetchInterviews = async () => {
    setLoading(true);
    try {
      const res = await api.get('/interviews/my-interviews');
      setInterviews(res.data || []);
    } catch (error) {
      showToast('Failed to load interview schedule', 'error');
    } finally {
      setLoading(false);
    }
  };

  const openViewModal = async (interview) => {
    try {
      const res = await api.get(`/interviews/${interview.id}`);
      setViewTargetInterview(res.data);
      setIsViewOpen(true);
    } catch (error) {
      showToast('Failed to load interview details', 'error');
    }
  };

  if (loading) {
    return <LoadingSpinner text="Loading your interview schedule..." />;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Interview Schedule</h1>
        <p className="text-sm text-gray-500">
          Check upcoming interview invitations, access meeting links, and review round details.
        </p>
      </div>

      {interviews.length === 0 ? (
        <div className="card p-12 text-center">
          <Calendar className="mx-auto text-gray-300 dark:text-gray-600 mb-3" size={40} />
          <h3 className="font-bold text-gray-800 dark:text-gray-200">No Interviews Scheduled Yet</h3>
          <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
            Once employers review and shortlist your applications, interview invitations will appear right here.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {interviews.map(item => (
            <div key={item.id} className="card p-5 border border-gray-200 dark:border-gray-800 hover:shadow-md transition flex flex-col justify-between">
              <div>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h3 className="font-bold text-gray-900 dark:text-white text-base">
                      {item.jobTitle || 'Technical Interview'}
                    </h3>
                    <p className="text-xs text-gray-500 flex items-center gap-1 mt-0.5">
                      <Building2 size={13} className="text-primary-600" />
                      {item.companyName || 'Verified Employer'}
                    </p>
                  </div>
                  <StatusBadge status={item.status} />
                </div>

                <div className="mt-4 p-3 bg-gray-50 dark:bg-gray-800/50 rounded-lg space-y-2 border border-gray-100 dark:border-gray-800 text-xs">
                  <div className="flex items-center gap-2 text-gray-700 dark:text-gray-300">
                    <Calendar size={14} className="text-primary-600" />
                    <span className="font-semibold">Date & Time:</span>
                    <span>{new Date(item.scheduledAt).toLocaleString()}</span>
                  </div>

                  <div className="flex items-center gap-2 text-gray-700 dark:text-gray-300">
                    <Video size={14} className="text-primary-600" />
                    <span className="font-semibold">Format:</span>
                    <span className="badge badge-info text-[10px]">{item.interviewType || 'ONLINE'}</span>
                  </div>

                  {item.location && (
                    <div className="flex items-center gap-2 text-gray-700 dark:text-gray-300">
                      <MapPin size={14} className="text-primary-600" />
                      <span className="font-semibold">Location / Office:</span>
                      <span>{item.location}</span>
                    </div>
                  )}

                  {item.notes && (
                    <div className="pt-2 border-t border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-400">
                      <div className="flex items-center gap-1 font-semibold text-gray-700 dark:text-gray-300 mb-1">
                        <FileText size={12} /> Employer Notes:
                      </div>
                      <p className="italic">{item.notes}</p>
                    </div>
                  )}
                </div>
              </div>

              <div className="mt-4 pt-3 border-t border-gray-100 dark:border-gray-800 flex justify-end gap-2">
                <button
                  onClick={() => openViewModal(item)}
                  className="btn btn-sm btn-outline flex items-center gap-1"
                  title="View Details"
                >
                  <Eye size={14} /> Details
                </button>
                {item.meetingLink ? (
                  <a
                    href={item.meetingLink.startsWith('http') ? item.meetingLink : `https://${item.meetingLink}`}
                    target="_blank"
                    rel="noreferrer"
                    className="btn btn-primary btn-sm flex items-center gap-1 shadow"
                  >
                    <Video size={14} /> Join Video Call <ExternalLink size={12} />
                  </a>
                ) : (
                  <span className="text-xs text-gray-400 italic">No video link required (In-person)</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* View Interview Details Modal */}
      <Modal
        isOpen={isViewOpen}
        onClose={() => setIsViewOpen(false)}
        title="Interview Details"
      >
        {viewTargetInterview && (
          <div className="space-y-4">
            <div className="p-4 bg-gray-50 dark:bg-gray-800/50 rounded-lg space-y-3">
              <div className="flex items-center gap-2 text-sm">
                <Building2 size={16} className="text-primary-600" />
                <span className="font-semibold text-gray-700 dark:text-gray-300">Company:</span>
                <span className="text-gray-900 dark:text-white">{viewTargetInterview.companyName}</span>
              </div>

              <div className="flex items-center gap-2 text-sm">
                <FileText size={16} className="text-primary-600" />
                <span className="font-semibold text-gray-700 dark:text-gray-300">Position:</span>
                <span className="text-gray-900 dark:text-white">{viewTargetInterview.jobTitle}</span>
              </div>

              <div className="flex items-center gap-2 text-sm">
                <Calendar size={16} className="text-primary-600" />
                <span className="font-semibold text-gray-700 dark:text-gray-300">Scheduled:</span>
                <span className="text-gray-900 dark:text-white">
                  {new Date(viewTargetInterview.scheduledAt).toLocaleString()}
                </span>
              </div>

              <div className="flex items-center gap-2 text-sm">
                <Video size={16} className="text-primary-600" />
                <span className="font-semibold text-gray-700 dark:text-gray-300">Format:</span>
                <span className="badge badge-info text-xs">{viewTargetInterview.interviewType || 'ONLINE'}</span>
              </div>

              {viewTargetInterview.location && (
                <div className="flex items-center gap-2 text-sm">
                  <MapPin size={16} className="text-primary-600" />
                  <span className="font-semibold text-gray-700 dark:text-gray-300">Location:</span>
                  <span className="text-gray-900 dark:text-white">{viewTargetInterview.location}</span>
                </div>
              )}

              <div className="flex items-center gap-2 text-sm">
                <CheckCircle size={16} className="text-primary-600" />
                <span className="font-semibold text-gray-700 dark:text-gray-300">Status:</span>
                <StatusBadge status={viewTargetInterview.status} />
              </div>

              {viewTargetInterview.notes && (
                <div className="pt-2 border-t border-gray-200 dark:border-gray-700">
                  <div className="flex items-center gap-1 font-semibold text-gray-700 dark:text-gray-300 mb-1">
                    <FileText size={14} /> Employer Instructions:
                  </div>
                  <p className="text-sm text-gray-600 dark:text-gray-400 italic">{viewTargetInterview.notes}</p>
                </div>
              )}
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-gray-200 dark:border-gray-800">
              <button
                onClick={() => setIsViewOpen(false)}
                className="btn btn-outline"
              >
                Close
              </button>
              {viewTargetInterview.meetingLink && (
                <a
                  href={viewTargetInterview.meetingLink.startsWith('http') ? viewTargetInterview.meetingLink : `https://${viewTargetInterview.meetingLink}`}
                  target="_blank"
                  rel="noreferrer"
                  className="btn btn-primary flex items-center gap-1"
                >
                  <Video size={16} /> Join Interview <ExternalLink size={14} />
                </a>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
