package org.auscope.portal.server.web.controllers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import junit.framework.Assert;

import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.auscope.portal.core.cloud.ComputeType;
import org.auscope.portal.core.cloud.StagedFile;
import org.auscope.portal.core.server.PortalPropertyPlaceholderConfigurer;
import org.auscope.portal.core.server.security.oauth2.PortalUser;
import org.auscope.portal.core.services.PortalServiceException;
import org.auscope.portal.core.services.cloud.CloudComputeService;
import org.auscope.portal.core.services.cloud.CloudStorageService;
import org.auscope.portal.core.services.cloud.FileStagingService;
import org.auscope.portal.core.test.ResourceUtil;
import org.auscope.portal.core.util.structure.Job;
import org.auscope.portal.server.vegl.VEGLJob;
import org.auscope.portal.server.vegl.VEGLJobManager;
import org.auscope.portal.server.vegl.VGLJobStatusAndLogReader;
import org.auscope.portal.server.vegl.VGLPollingJobQueueManager;
import org.auscope.portal.server.vegl.VglDownload;
import org.auscope.portal.server.vegl.VglMachineImage;
import org.auscope.portal.server.vegl.VglParameter;
import org.auscope.portal.server.vegl.mail.JobMailSender;
import org.auscope.portal.server.web.service.monitor.VGLJobStatusChangeHandler;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.ModelMap;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * Unit tests for JobBuilderController
 * @author Josh Vote
 *
 */
public class TestJobBuilderController {
    private Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);
    }};



    private VEGLJobManager mockJobManager;
    private FileStagingService mockFileStagingService;
    private CloudStorageService[] mockCloudStorageServices;
    private PortalPropertyPlaceholderConfigurer mockHostConfigurer;
    private CloudComputeService[] mockCloudComputeServices;
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;
    private HttpSession mockSession;
    private PortalUser mockPortalUser;
    private VGLPollingJobQueueManager vglPollingJobQueueManager;
    private VGLJobStatusChangeHandler vglJobStatusChangeHandler;



    private JobMailSender mockJobMailSender;
    private VGLJobStatusAndLogReader mockVGLJobStatusAndLogReader;


    private JobBuilderController controller;

    @Before
    public void init() {
        //Mock objects required for Object Under Test
        mockJobManager = context.mock(VEGLJobManager.class);
        mockFileStagingService = context.mock(FileStagingService.class);
        mockHostConfigurer = context.mock(PortalPropertyPlaceholderConfigurer.class);
        mockPortalUser = context.mock(PortalUser.class);
        mockCloudStorageServices = new CloudStorageService[] {context.mock(CloudStorageService.class)};
        mockCloudComputeServices = new CloudComputeService[] {context.mock(CloudComputeService.class)};
        mockRequest = context.mock(HttpServletRequest.class);
        mockResponse = context.mock(HttpServletResponse.class);
        mockSession = context.mock(HttpSession.class);


        mockJobMailSender = context.mock(JobMailSender.class);
        mockVGLJobStatusAndLogReader = context.mock(VGLJobStatusAndLogReader.class);


        vglJobStatusChangeHandler = new VGLJobStatusChangeHandler(mockJobManager,mockJobMailSender,mockVGLJobStatusAndLogReader);
        vglPollingJobQueueManager = new VGLPollingJobQueueManager();
        //Object Under Test
        controller = new JobBuilderController(mockJobManager, mockFileStagingService, mockHostConfigurer, mockCloudStorageServices, mockCloudComputeServices, vglJobStatusChangeHandler,vglPollingJobQueueManager);
    }

    @After
    public void destroy(){
        vglPollingJobQueueManager.getQueue().clear();
    }



    /**
     * Tests that retrieving job object succeeds.
     */
    @Test
    public void testGetJobObject() {
        final String jobId = "1";
        final VEGLJob jobObj = new VEGLJob(new Integer(jobId));

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(Integer.parseInt(jobId));
            will(returnValue(jobObj));
        }});

        ModelAndView mav = controller.getJobObject(jobId);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));
        Assert.assertNotNull(mav.getModel().get("data"));
    }

    /**
     * Tests that retrieving job object fails when the
     * underlying job manager's job query service fails.
     */
    @Test
    public void testGetJobObject_Exception() {
        final String jobId = "1";

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(Integer.parseInt(jobId));
            will(throwException(new Exception()));
        }});

        ModelAndView mav = controller.getJobObject(jobId);
        Assert.assertFalse((Boolean)mav.getModel().get("success"));
    }

    /**
     * Tests that the retrieving of a list of job file object
     * succeeds.
     * @throws Exception
     */
    @Test
    public void testListJobFiles() throws Exception {
        final String jobId = "1";
        final VEGLJob mockJob = new VEGLJob(new Integer(jobId));

        final File mockFile1 = context.mock(File.class, "MockFile1");
        final File mockFile2 = context.mock(File.class, "MockFile2");
        final StagedFile[] mockStageFiles = new StagedFile[] {
                new StagedFile(mockJob, "mockFile1", mockFile1),
                new StagedFile(mockJob, "mockFile2", mockFile2) };

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(Integer.parseInt(jobId));
            will(returnValue(mockJob));
            //We should have a call to file staging service to get our files
            oneOf(mockFileStagingService).listStageInDirectoryFiles(mockJob);
            will(returnValue(mockStageFiles));

            oneOf(mockFile1).length();will(returnValue(512L));
            oneOf(mockFile2).length();will(returnValue(512L));
        }});

        ModelAndView mav = controller.listJobFiles(jobId);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));
        Assert.assertNotNull(mav.getModel().get("data"));
    }

    /**
     * Tests that the retrieving of job files fails
     * when the job cannot be found.
     */
    @Test
    public void testListJobFiles_JobNotFound() {
        final String jobId = "1";

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(Integer.parseInt(jobId));
            will(throwException(new Exception()));
        }});

        ModelAndView mav = controller.listJobFiles(jobId);
        Assert.assertFalse((Boolean)mav.getModel().get("success"));
        Assert.assertNull(mav.getModel().get("data"));
    }

    /**
     * Tests that the retrieving of job files fails
     * when the underlying file staging service fails.
     * @throws Exception
     */
    @Test
    public void testListJobFiles_Exception() throws Exception {
        final String jobId = "1";
        final VEGLJob mockJob = new VEGLJob(new Integer(jobId));

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(Integer.parseInt(jobId));
            will(returnValue(mockJob));
            //We should have a call to file staging service to get our files
            oneOf(mockFileStagingService).listStageInDirectoryFiles(mockJob);
            will(throwException(new PortalServiceException("test exception","test exception")));
        }});

        ModelAndView mav = controller.listJobFiles(jobId);
        Assert.assertFalse((Boolean)mav.getModel().get("success"));
        Assert.assertNull(mav.getModel().get("data"));
    }

    /**
     * Tests that the downloading of job file fails when
     * the underlying file staging service's file download handler fails.
     * @throws Exception
     */
    @Test
    public void testDownloadFile() throws Exception {
        final String jobId = "1";
        final VEGLJob jobObj = new VEGLJob(new Integer(jobId));
        final String filename = "test.py";

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(jobObj.getId());
            will(returnValue(jobObj));
            //We should have a call to file staging service to download a file
            oneOf(mockFileStagingService).handleFileDownload(jobObj, filename, mockResponse);
        }});

        ModelAndView mav = controller.downloadFile(mockRequest, mockResponse, jobId, filename);
        Assert.assertNull(mav);
    }

    /**
     * Tests that the deleting of given job files succeeds.
     */
    @Test
    public void testDeleteFiles() {
        final String jobId = "1";
        final VEGLJob jobObj = new VEGLJob(new Integer(jobId));
        final String file1 = "file1.txt";
        final String file2 = "file2.txt";
        final String[] filenames = new String[] { file1, file2 };

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(jobObj.getId());
            will(returnValue(jobObj));
            //We should have calls to file staging service to delete files in staging dir
            oneOf(mockFileStagingService).deleteStageInFile(jobObj, file1);
            will(returnValue(true));
            oneOf(mockFileStagingService).deleteStageInFile(jobObj, file2);
            will(returnValue(true));
        }});

        ModelAndView mav = controller.deleteFiles(jobId, filenames);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));
        Assert.assertNull(mav.getModel().get("data"));
    }

    /**
     * Tests that the deleting of given job files fails
     * when the job cannot be found.
     */
    @Test
    public void testDeleteFiles_JobNotFoundException() {
        final String jobId = "1";
        final String file1 = "file1.txt";
        final String file2 = "file2.txt";
        final String[] filenames = new String[] { file1, file2 };

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(Integer.parseInt(jobId));
            will(throwException(new Exception()));
        }});

        ModelAndView mav = controller.deleteFiles(jobId, filenames);
        Assert.assertFalse((Boolean)mav.getModel().get("success"));
        Assert.assertNull(mav.getModel().get("data"));
    }

    /**
     * Tests that the deleting of a given job's download objects succeeds.
     */
    @Test
    public void testDeleteDownloads() {
        final String jobId = "1";
        final VEGLJob jobObj = new VEGLJob(new Integer(jobId));
        int downloadId1 = 13579;
        int downloadId2 = 23480;
        final Integer[] downloadIds = new Integer[] { downloadId1, downloadId2 };
        VglDownload vglDownload1 = new VglDownload(downloadId1);
        VglDownload vglDownload2 = new VglDownload(downloadId2);
        VglDownload[] vglDownloads = new VglDownload[] { vglDownload1, vglDownload2 };
        final List<VglDownload> downloadList = new ArrayList<VglDownload>(Arrays.asList(vglDownloads));
        jobObj.setJobDownloads(downloadList);

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(jobObj.getId());
            will(returnValue(jobObj));
            //We should have a call to our job manager to save our job object
            oneOf(mockJobManager).saveJob(jobObj);
        }});

        ModelAndView mav = controller.deleteDownloads(jobId, downloadIds);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));
        Assert.assertNull(mav.getModel().get("data"));
    }

    /**
     * Tests that the deleting of a given job's download objects fails
     * when job saving fails.
     */
    @Test
    public void testDeleteDownloads_SaveJobException() {
        final String jobId = "1";
        final VEGLJob jobObj = new VEGLJob(new Integer(jobId));
        int downloadId1 = 13579;
        int downloadId2 = 23480;
        final Integer[] downloadIds = new Integer[] { downloadId1, downloadId2 };
        VglDownload vglDownload1 = new VglDownload(downloadId1);
        VglDownload vglDownload2 = new VglDownload(downloadId2);
        VglDownload[] vglDownloads = new VglDownload[] { vglDownload1, vglDownload2 };
        final List<VglDownload> downloadList = new ArrayList<VglDownload>(Arrays.asList(vglDownloads));
        jobObj.setJobDownloads(downloadList);

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(jobObj.getId());
            will(returnValue(jobObj));
            //We should have a call to our job manager to save our job object
            oneOf(mockJobManager).saveJob(jobObj);
            will(throwException(new Exception()));
        }});

        ModelAndView mav = controller.deleteDownloads(jobId, downloadIds);
        Assert.assertFalse((Boolean)mav.getModel().get("success"));
        Assert.assertNull(mav.getModel().get("data"));
    }

    /**
     * Tests that the deleting of a given job's download objects fails
     * when the job cannot be found.
     */
    @Test
    public void testDeleteDownloads_JobNotFoundException() {
        final String jobId = "1";
        int downloadId1 = 13579;
        int downloadId2 = 23480;
        final Integer[] downloadIds = new Integer[] { downloadId1, downloadId2 };

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(Integer.parseInt(jobId));
            will(throwException(new Exception()));
        }});

        ModelAndView mav = controller.deleteDownloads(jobId, downloadIds);
        Assert.assertFalse((Boolean)mav.getModel().get("success"));
        Assert.assertNull(mav.getModel().get("data"));
    }

    /**
     * Tests that file uploading for a given job succeeds.
     * @throws Exception
     */
    @Test
    public void testUploadFile() throws Exception {
        final VEGLJob jobObj = new VEGLJob(new Integer(13));
        final MultipartHttpServletRequest mockMultipartRequest = context.mock(MultipartHttpServletRequest.class);
        final File mockFile = context.mock(File.class);
        final StagedFile mockStagedFile = new StagedFile(jobObj, "mockFile", mockFile);

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(jobObj.getId());
            will(returnValue(jobObj));

            //We should have a call to file staging service to update a file
            oneOf(mockFileStagingService).handleFileUpload(jobObj, mockMultipartRequest);
            will(returnValue(mockStagedFile));

            oneOf(mockFile).length();
            will(returnValue(1024L));
        }});

        ModelAndView mav = controller.uploadFile(mockMultipartRequest, mockResponse, jobObj.getId().toString());
        Assert.assertTrue((Boolean)mav.getModel().get("success"));
    }

    /**
     * Tests that file uploading for a given job fails
     * when the underlying file staging file upload handler fails.
     * @throws Exception
     */
    @Test
    public void testUploadFile_FileUploadException() throws Exception {
        final VEGLJob jobObj = new VEGLJob(new Integer(13));
        final MultipartHttpServletRequest mockMultipartRequest = context.mock(MultipartHttpServletRequest.class);

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(jobObj.getId());
            will(returnValue(jobObj));

            //We should have a call to file staging service to update a file
            oneOf(mockFileStagingService).handleFileUpload(jobObj, mockMultipartRequest);
            will(throwException(new PortalServiceException("Test Exception","Test Exception")));
        }});

        ModelAndView mav = controller.uploadFile(mockMultipartRequest, mockResponse, jobObj.getId().toString());
        Assert.assertFalse((Boolean)mav.getModel().get("success"));
    }

    /**
     * Tests that file uploading for a given job fails
     * when the given job cannot be found.
     * @throws Exception
     */
    @Test
    public void testUploadFile_JobNotFoundException() throws Exception {
        final VEGLJob jobObj = new VEGLJob(new Integer(13));
        final MultipartHttpServletRequest mockMultipartRequest = context.mock(MultipartHttpServletRequest.class);

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(jobObj.getId());
            will(throwException(new Exception()));
        }});

        ModelAndView mav = controller.uploadFile(mockMultipartRequest, mockResponse, jobObj.getId().toString());
        Assert.assertFalse((Boolean)mav.getModel().get("success"));
        Assert.assertNull(mav.getModel().get("data"));
    }

    /**
     * Tests that retrieving of a given job's download objects succeeds.
     */
    @Test
    public void testGetJobDownloads() {
        final int jobId = 1;
        final VEGLJob mockJob = context.mock(VEGLJob.class);
        final VglDownload[] existingDownloads = new VglDownload[] { new VglDownload(12356) };
        final List<VglDownload> downloadList = new ArrayList<VglDownload>(Arrays.asList(existingDownloads));

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(jobId);
            will(returnValue(mockJob));
            //We should have a call to job object to get a list of download objects
            oneOf(mockJob).getJobDownloads();
            will(returnValue(downloadList));
        }});

        ModelAndView mav = controller.getJobDownloads(jobId);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));
        Assert.assertNotNull(mav.getModel().get("data"));
    }

    /**
     * Tests that retrieving of a given job's download objects fails
     * when the given job cannot be found.
     */
    @Test
    public void testGetJobDownloads_Exception() {
        final int jobId = 1;

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(jobId);
            will(throwException(new Exception()));
        }});

        ModelAndView mav = controller.getJobDownloads(jobId);
        Assert.assertFalse((Boolean)mav.getModel().get("success"));
        Assert.assertNull(mav.getModel().get("data"));
    }

    /**
     * Tests that the getting of job status succeeds.
     */
    @Test
    public void testGetJobStatus() {
        final String jobId = "1";
        final String expectedStatus = "Pending";
        final VEGLJob mockJob = context.mock(VEGLJob.class);

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(Integer.parseInt(jobId));
            will(returnValue(mockJob));

            oneOf(mockJob).getStatus();
            will(returnValue(expectedStatus));
        }});

        ModelAndView mav = controller.getJobStatus(jobId);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));
        String jobStatus = (String)mav.getModel().get("data");
        Assert.assertEquals(expectedStatus, jobStatus);
    }

    /**
     * Tests that the retrieving of job status fails
     * when the job cannot be found.
     */
    @Test
    public void testGetJobStatus_JobNotFound() {
        final String jobId = "1";

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(Integer.parseInt(jobId));
            will(throwException(new Exception()));
        }});

        ModelAndView mav = controller.getJobStatus(jobId);
        Assert.assertFalse((Boolean)mav.getModel().get("success"));
        Assert.assertNull(mav.getModel().get("data"));
    }

    /**
     * Tests that cancelling submission succeeds.
     * @throws Exception
     */
    @Test
    public void testCancelSubmission() throws Exception {
        final String jobId = "1";
        final VEGLJob mockJob = new VEGLJob(new Integer(jobId));

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(Integer.parseInt(jobId));
            will(returnValue(mockJob));
            //We should have a call to file staging service to get our files
            oneOf(mockFileStagingService).deleteStageInDirectory(mockJob);
            will(returnValue(true));
        }});

        ModelAndView mav = controller.cancelSubmission(jobId);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));
        Assert.assertNull(mav.getModel().get("data"));
    }

    /**
     * Tests that cancelling job submission fails when the
     * job cannot be found.
     * @throws Exception
     */
    @Test
    public void testCancelSubmission_Exception() throws Exception {
        final String jobId = "1";

        context.checking(new Expectations() {{
            //We should have a call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(Integer.parseInt(jobId));
            will(throwException(new Exception()));
        }});

        ModelAndView mav = controller.cancelSubmission(jobId);
        Assert.assertFalse((Boolean)mav.getModel().get("success"));
        Assert.assertNull(mav.getModel().get("data"));
    }

    /**
     * Tests that job submission correctly interacts with all dependencies
     * @throws Exception
     */
    @Test
    public void testJobSubmission() throws Exception {
        //Instantiate our job object
        final VEGLJob jobObj = new VEGLJob(new Integer(13));
        final File mockFile1 = context.mock(File.class, "MockFile1");
        final File mockFile2 = context.mock(File.class, "MockFile2");
        final StagedFile[] stageInFiles = new StagedFile[] {new StagedFile(jobObj, "mockFile1", mockFile1), new StagedFile(jobObj, "mockFile2", mockFile2)};
        final String computeVmId = "compute-vmi-id";
        final String computeServiceId = "compute-service-id";
        final String instanceId = "new-instance-id";
        final Sequence jobFileSequence = context.sequence("jobFileSequence"); //this makes sure we aren't deleting directories before uploading (and other nonsense)
        final OutputStream mockOutputStream = context.mock(OutputStream.class);
        final String jobInSavedState = JobBuilderController.STATUS_UNSUBMITTED;
        final VglMachineImage[] mockImages = new VglMachineImage[] {context.mock(VglMachineImage.class)};
        final String storageBucket = "storage-bucket";
        final String storageAccess = "213-asd-54";
        final String storageSecret = "tops3cret";
        final String storageServiceId = "storageid";
        final String storageEndpoint = "http://example.org";
        final String storageProvider = "provider";
        final String storageAuthVersion = "1.2.3";
        final String regionName = null;

        jobObj.setComputeVmId(computeVmId);
        jobObj.setStatus(jobInSavedState); // by default, the job is in SAVED state
        jobObj.setStorageBaseKey("base/key");
        jobObj.setComputeServiceId(computeServiceId);
        jobObj.setStorageServiceId(storageServiceId);

        context.checking(new Expectations() {{
            //We should have access control check to ensure user has permission to run the job
            oneOf(mockCloudComputeServices[0]).getAvailableImages();will(returnValue(mockImages));
            oneOf(mockImages[0]).getImageId();will(returnValue("compute-vmi-id"));
            oneOf(mockImages[0]).getPermissions();will(returnValue(new String[] {"testRole2"}));
            allowing(mockRequest).isUserInRole("testRole2");will(returnValue(true));

            //We should have 1 call to our job manager to get our job object and 1 call to save it
            oneOf(mockJobManager).getJobById(jobObj.getId());will(returnValue(jobObj));
            oneOf(mockJobManager).saveJob(jobObj);

            oneOf(mockFileStagingService).writeFile(jobObj, JobBuilderController.DOWNLOAD_SCRIPT);
            will(returnValue(mockOutputStream));
            allowing(mockOutputStream).close();

            //We should have 1 call to get our stage in files
            oneOf(mockFileStagingService).listStageInDirectoryFiles(jobObj);will(returnValue(stageInFiles));
            inSequence(jobFileSequence);

            //We allow calls to the Configurer which simply extract values from our property file
            allowing(mockHostConfigurer).resolvePlaceholder(with(any(String.class)));

            allowing(mockCloudStorageServices[0]).getId();will(returnValue(storageServiceId));
            allowing(mockCloudStorageServices[0]).getBucket();will(returnValue(storageBucket));
            allowing(mockCloudStorageServices[0]).getAccessKey();will(returnValue(storageAccess));
            allowing(mockCloudStorageServices[0]).getSecretKey();will(returnValue(storageSecret));
            allowing(mockCloudStorageServices[0]).getProvider();will(returnValue(storageProvider));
            allowing(mockCloudStorageServices[0]).getProvider();will(returnValue(storageProvider));
            allowing(mockCloudStorageServices[0]).getAuthVersion();will(returnValue(storageAuthVersion));
            allowing(mockCloudStorageServices[0]).getEndpoint();will(returnValue(storageEndpoint));
            allowing(mockCloudStorageServices[0]).getProvider();will(returnValue(storageProvider));
            allowing(mockCloudStorageServices[0]).getAuthVersion();will(returnValue(storageAuthVersion));
            allowing(mockCloudStorageServices[0]).getRegionName();will(returnValue(regionName));

            allowing(mockCloudComputeServices[0]).getId();will(returnValue(computeServiceId));

            //We should have 1 call to upload them
            oneOf(mockCloudStorageServices[0]).uploadJobFiles(with(equal(jobObj)), with(equal(new File[] {mockFile1, mockFile2})));
            inSequence(jobFileSequence);

            //And finally 1 call to execute the job
            oneOf(mockCloudComputeServices[0]).executeJob(with(any(VEGLJob.class)), with(any(String.class)));will(returnValue(instanceId));

            oneOf(mockJobManager).saveJob(jobObj);

            //We should have 1 call to our job manager to create a job audit trail record
            oneOf(mockJobManager).createJobAuditTrail(jobInSavedState, jobObj, "Set job to provisioning");
            oneOf(mockJobManager).createJobAuditTrail("Provisioning", jobObj, "Set job to Pending");

        }});


        ModelAndView mav = controller.submitJob(mockRequest, mockResponse, jobObj.getId().toString());

        Assert.assertTrue((Boolean)mav.getModel().get("success"));
        Thread.sleep(1000);
        Assert.assertEquals(instanceId, jobObj.getComputeInstanceId());
        Assert.assertEquals(JobBuilderController.STATUS_PENDING, jobObj.getStatus());
        Assert.assertNotNull(jobObj.getSubmitDate());
    }

    /**
     * Tests that job submission fails correctly when user doesn't have permission to use
     * the VMI.
     */
    @Test
    public void testJobSubmission_PermissionDenied() throws Exception {
        //Instantiate our job object
        final VEGLJob jobObj = new VEGLJob(new Integer(13));
        final String computeServiceId = "ccsid";
        final String injectedComputeVmId = "injected-compute-vmi-id";
        final String jobInSavedState = JobBuilderController.STATUS_UNSUBMITTED;
        final VglMachineImage[] mockImages = new VglMachineImage[] {context.mock(VglMachineImage.class)};
        final String errorDescription = "You do not have the permission to submit this job for processing.";
        final String storageServiceId = "cssid";

        jobObj.setComputeVmId(injectedComputeVmId);
        jobObj.setStatus(jobInSavedState); // by default, the job is in SAVED state
        jobObj.setComputeServiceId(computeServiceId);
        jobObj.setStorageServiceId(storageServiceId);


        context.checking(new Expectations() {{
            //We should have 1 call to our job manager to get our job object and 1 call to save it
            oneOf(mockJobManager).getJobById(jobObj.getId());will(returnValue(jobObj));
            oneOf(mockJobManager).saveJob(jobObj);

            allowing(mockCloudComputeServices[0]).getId();will(returnValue(computeServiceId));
            allowing(mockCloudStorageServices[0]).getId();will(returnValue(storageServiceId));

            //We should have access control check to ensure user has permission to run the job
            oneOf(mockRequest).getSession();will(returnValue(mockSession));
            allowing(mockRequest).isUserInRole("a-different-role");will(returnValue(false));
            oneOf(mockCloudComputeServices[0]).getAvailableImages();will(returnValue(mockImages));
            oneOf(mockImages[0]).getImageId();will(returnValue("compute-vmi-id"));
            oneOf(mockImages[0]).getPermissions();will(returnValue(new String[] {"a-different-role"}));

            //We should have 1 call to our job manager to create a job audit trail record
            oneOf(mockJobManager).createJobAuditTrail(jobInSavedState, jobObj, errorDescription);
        }});

        ModelAndView mav = controller.submitJob(mockRequest, mockResponse, jobObj.getId().toString());

        Assert.assertFalse((Boolean)mav.getModel().get("success"));
        Assert.assertEquals(JobBuilderController.STATUS_UNSUBMITTED, jobObj.getStatus());
    }

    /**
     * Tests that job submission fails correctly when the job doesn't exist
     * @throws Exception
     */
    @Test
    public void testJobSubmission_JobDNE() throws Exception {
        final String jobId = "24";
        context.checking(new Expectations() {{
            //We should have 1 call to our job manager to get our job object and 1 call to save it
            oneOf(mockJobManager).getJobById(Integer.parseInt(jobId));will(returnValue(null));
        }});

        ModelAndView mav = controller.submitJob(mockRequest, mockResponse, jobId);

        Assert.assertFalse((Boolean)mav.getModel().get("success"));
    }

    /**
     * Tests that job submission fails correctly when files cannot be uploaded to S3
     * @throws Exception
     */
    @Test
    public void testJobSubmission_S3Failure() throws Exception {
        //Instantiate our job object
        final VEGLJob jobObj = new VEGLJob(13);
        final String computeVmId = "compute-vmi-id";
        final File mockFile1 = context.mock(File.class, "MockFile1");
        final File mockFile2 = context.mock(File.class, "MockFile2");
        final StagedFile[] stageInFiles = new StagedFile[] {new StagedFile(jobObj, "mockFile1", mockFile1), new StagedFile(jobObj, "mockFile2", mockFile2)};
        final String jobInSavedState = JobBuilderController.STATUS_UNSUBMITTED;
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
        final VglMachineImage[] mockImages = new VglMachineImage[] {context.mock(VglMachineImage.class)};
        final String computeServiceId = "id-1";
        final String storageServiceId = "id-2";
        jobObj.setComputeVmId(computeVmId);
        jobObj.setStatus(jobInSavedState); // by default, the job is in SAVED state
        jobObj.setComputeServiceId(computeServiceId);
        jobObj.setStorageServiceId(storageServiceId);


        context.checking(new Expectations() {{
            //We should have 1 call to our job manager to get our job object
            oneOf(mockJobManager).getJobById(jobObj.getId());will(returnValue(jobObj));

            //We should have access control check to ensure user has permission to run the job
            oneOf(mockCloudComputeServices[0]).getAvailableImages();will(returnValue(mockImages));
            oneOf(mockImages[0]).getImageId();will(returnValue("compute-vmi-id"));
            oneOf(mockImages[0]).getPermissions();will(returnValue(new String[] {"testRole1"}));
            allowing(mockRequest).isUserInRole("testRole1");will(returnValue(true));


            oneOf(mockFileStagingService).writeFile(jobObj, JobBuilderController.DOWNLOAD_SCRIPT);
            will(returnValue(bos));

            allowing(mockCloudComputeServices[0]).getId();will(returnValue(computeServiceId));
            allowing(mockCloudStorageServices[0]).getId();will(returnValue(storageServiceId));

            //We should have 1 call to get our stage in files
            oneOf(mockFileStagingService).listStageInDirectoryFiles(jobObj);will(returnValue(stageInFiles));

            //And one call to upload them (which we will mock as failing)
            oneOf(mockCloudStorageServices[0]).uploadJobFiles(with(equal(jobObj)), with(any(File[].class)));will(throwException(new PortalServiceException("")));

            //We should have 1 call to our job manager to create a job audit trail record
            oneOf(mockJobManager).createJobAuditTrail(jobInSavedState, jobObj, "");
        }});

        ModelAndView mav = controller.submitJob(mockRequest, mockResponse, jobObj.getId().toString());

        Assert.assertFalse((Boolean)mav.getModel().get("success"));
        Assert.assertEquals(JobBuilderController.STATUS_UNSUBMITTED, jobObj.getStatus());
    }

    /**
     * Tests that job submission fails correctly when files cannot be uploaded to S3
     * @throws Exception
     */
    @Test
    public void testJobSubmission_ExecuteFailure() throws Exception {
        //Instantiate our job object
        final VEGLJob jobObj = new VEGLJob(13);
        final String jobInSavedState = JobBuilderController.STATUS_UNSUBMITTED;

        final String computeVmId = "compute-vmi-id";
        final String computeServiceId = "compute-service-id";

        final File mockFile1 = context.mock(File.class, "MockFile1");
        final File mockFile2 = context.mock(File.class, "MockFile2");
        final StagedFile[] stageInFiles = new StagedFile[] {new StagedFile(jobObj, "mockFile1", mockFile1), new StagedFile(jobObj, "mockFile2", mockFile2)};
        final OutputStream mockOutputStream = context.mock(OutputStream.class);
        final VglMachineImage[] mockImages = new VglMachineImage[] {context.mock(VglMachineImage.class)};
        final String storageBucket = "storage-bucket";
        final String storageAccess = "213-asd-54";
        final String storageSecret = "tops3cret";
        final String storageProvider = "provider";
        final String storageAuthVersion = "1.2.3";
        final String storageEndpoint = "http://example.org";
        final String storageServiceId = "storage-service-id";
        final String regionName = "region-name";
        final PortalServiceException exception = new PortalServiceException("Some random error","Some error correction");

        jobObj.setComputeVmId(computeVmId);
        //As submitJob method no longer explicitly checks for empty storage credentials,
        //we need to manually set the storageBaseKey property to avoid NullPointerException
        jobObj.setStorageBaseKey("storageBaseKey");
        //By default, a job is in SAVED state
        jobObj.setStatus(jobInSavedState);
        jobObj.setComputeServiceId(computeServiceId);
        jobObj.setStorageServiceId(storageServiceId);

        context.checking(new Expectations() {{
            //We should have 1 call to our job manager to get our job object and 1 call to save it
            oneOf(mockJobManager).getJobById(jobObj.getId());will(returnValue(jobObj));

            oneOf(mockFileStagingService).writeFile(jobObj, JobBuilderController.DOWNLOAD_SCRIPT);
            will(returnValue(mockOutputStream));
            allowing(mockOutputStream).close();

            //We should have 1 call to get our stage in files
            oneOf(mockFileStagingService).listStageInDirectoryFiles(jobObj);will(returnValue(stageInFiles));

            //We allow calls to the Configurer which simply extract values from our property file
            allowing(mockHostConfigurer).resolvePlaceholder(with(any(String.class)));

            allowing(mockCloudStorageServices[0]).getId();will(returnValue(storageServiceId));
            allowing(mockCloudStorageServices[0]).getBucket();will(returnValue(storageBucket));
            allowing(mockCloudStorageServices[0]).getAccessKey();will(returnValue(storageAccess));
            allowing(mockCloudStorageServices[0]).getSecretKey();will(returnValue(storageSecret));
            allowing(mockCloudStorageServices[0]).getProvider();will(returnValue(storageProvider));
            allowing(mockCloudStorageServices[0]).getAuthVersion();will(returnValue(storageAuthVersion));
            allowing(mockCloudStorageServices[0]).getEndpoint();will(returnValue(storageEndpoint));
            allowing(mockCloudStorageServices[0]).getAuthVersion();will(returnValue(storageAuthVersion));
            allowing(mockCloudStorageServices[0]).getRegionName();will(returnValue(regionName));

            //We should have 1 call to upload them
            oneOf(mockCloudStorageServices[0]).uploadJobFiles(with(equal(jobObj)), with(any(File[].class)));

            //We should have access control check to ensure user has permission to run the job
            oneOf(mockCloudComputeServices[0]).getAvailableImages();will(returnValue(mockImages));
            oneOf(mockImages[0]).getImageId();will(returnValue("compute-vmi-id"));
            oneOf(mockImages[0]).getPermissions();will(returnValue(new String[] {"testRole1"}));
            allowing(mockRequest).isUserInRole("testRole1");will(returnValue(true));

            allowing(mockCloudComputeServices[0]).getId();will(returnValue(computeServiceId));

            allowing(mockJobManager).saveJob(jobObj);

            //And finally 1 call to execute the job (which will throw PortalServiceException indicating failure)
            oneOf(mockCloudComputeServices[0]).executeJob(with(any(VEGLJob.class)), with(any(String.class)));will(throwException(exception));

            //We should have 1 call to our job manager to create a job audit trail record
            oneOf(mockJobManager).createJobAuditTrail(jobInSavedState, jobObj, "Set job to provisioning");

            oneOf(mockJobManager).createJobAuditTrail(JobBuilderController.STATUS_PROVISION, jobObj, exception);

            oneOf(mockJobManager).createJobAuditTrail(JobBuilderController.STATUS_PROVISION, jobObj, "Job status updated.");

            oneOf(mockJobMailSender).sendMail(jobObj);
            oneOf(mockVGLJobStatusAndLogReader).getSectionedLog(jobObj, "Time");
        }});

        ModelAndView mav = controller.submitJob(mockRequest, mockResponse, jobObj.getId().toString());

        Assert.assertTrue((Boolean)mav.getModel().get("success"));
        //VT:wait a while for the thread to finish before getting the status.
        Thread.sleep(1000);

        Assert.assertEquals(JobBuilderController.STATUS_ERROR, jobObj.getStatus());
        Assert.assertTrue(jobObj.getProcessDate()!=null);

    }





    /**
     * Tests that job submission fails correctly when files cannot be uploaded to S3
     * @throws Exception
     */
    @Test
    public void testJobSubmissionWithQueue_ExecuteFailure() throws Exception {
        //Instantiate our job object
        final VEGLJob jobObj = new VEGLJob(13);
        final String jobInSavedState = JobBuilderController.STATUS_UNSUBMITTED;

        final String computeVmId = "compute-vmi-id";
        final String computeServiceId = "compute-service-id";

        final File mockFile1 = context.mock(File.class, "MockFile1");
        final File mockFile2 = context.mock(File.class, "MockFile2");
        final StagedFile[] stageInFiles = new StagedFile[] {new StagedFile(jobObj, "mockFile1", mockFile1), new StagedFile(jobObj, "mockFile2", mockFile2)};
        final OutputStream mockOutputStream = context.mock(OutputStream.class);
        final VglMachineImage[] mockImages = new VglMachineImage[] {context.mock(VglMachineImage.class)};
        final String storageBucket = "storage-bucket";
        final String storageAccess = "213-asd-54";
        final String storageSecret = "tops3cret";
        final String storageProvider = "provider";
        final String storageAuthVersion = "1.2.3";
        final String storageEndpoint = "http://example.org";
        final String storageServiceId = "storage-service-id";
        final String regionName = "region-name";

        jobObj.setComputeVmId(computeVmId);
        //As submitJob method no longer explicitly checks for empty storage credentials,
        //we need to manually set the storageBaseKey property to avoid NullPointerException
        jobObj.setStorageBaseKey("storageBaseKey");
        //By default, a job is in SAVED state
        jobObj.setStatus(jobInSavedState);
        jobObj.setComputeServiceId(computeServiceId);
        jobObj.setStorageServiceId(storageServiceId);

        context.checking(new Expectations() {{
            //We should have 1 call to our job manager to get our job object and 1 call to save it
            oneOf(mockJobManager).getJobById(jobObj.getId());will(returnValue(jobObj));

            oneOf(mockFileStagingService).writeFile(jobObj, JobBuilderController.DOWNLOAD_SCRIPT);
            will(returnValue(mockOutputStream));
            allowing(mockOutputStream).close();

            //We should have 1 call to get our stage in files
            oneOf(mockFileStagingService).listStageInDirectoryFiles(jobObj);will(returnValue(stageInFiles));

            //We allow calls to the Configurer which simply extract values from our property file
            allowing(mockHostConfigurer).resolvePlaceholder(with(any(String.class)));

            allowing(mockCloudStorageServices[0]).getId();will(returnValue(storageServiceId));
            allowing(mockCloudStorageServices[0]).getBucket();will(returnValue(storageBucket));
            allowing(mockCloudStorageServices[0]).getAccessKey();will(returnValue(storageAccess));
            allowing(mockCloudStorageServices[0]).getSecretKey();will(returnValue(storageSecret));
            allowing(mockCloudStorageServices[0]).getProvider();will(returnValue(storageProvider));
            allowing(mockCloudStorageServices[0]).getAuthVersion();will(returnValue(storageAuthVersion));
            allowing(mockCloudStorageServices[0]).getEndpoint();will(returnValue(storageEndpoint));
            allowing(mockCloudStorageServices[0]).getAuthVersion();will(returnValue(storageAuthVersion));
            allowing(mockCloudStorageServices[0]).getRegionName();will(returnValue(regionName));

            //We should have 1 call to upload them
            oneOf(mockCloudStorageServices[0]).uploadJobFiles(with(equal(jobObj)), with(any(File[].class)));

            //We should have access control check to ensure user has permission to run the job
            oneOf(mockCloudComputeServices[0]).getAvailableImages();will(returnValue(mockImages));
            oneOf(mockImages[0]).getImageId();will(returnValue("compute-vmi-id"));
            oneOf(mockImages[0]).getPermissions();will(returnValue(new String[] {"testRole1"}));
            allowing(mockRequest).isUserInRole("testRole1");will(returnValue(true));

            allowing(mockJobManager).saveJob(jobObj);

            allowing(mockCloudComputeServices[0]).getId();will(returnValue(computeServiceId));

            //And finally 1 call to execute the job (which will throw PortalServiceException indicating failure)
            oneOf(mockCloudComputeServices[0]).executeJob(with(any(VEGLJob.class)), with(any(String.class)));will(throwException(new PortalServiceException("Some random error","Some error correction with Quota exceeded")));

            //We should have 1 call to our job manager to create a job audit trail record
            oneOf(mockJobManager).createJobAuditTrail(jobInSavedState, jobObj, "Set job to provisioning");

            oneOf(mockJobManager).createJobAuditTrail(JobBuilderController.STATUS_PROVISION, jobObj, "Job Placed in Queue");

        }});

        ModelAndView mav = controller.submitJob(mockRequest, mockResponse, jobObj.getId().toString());
        Thread.sleep(2000);
        Assert.assertTrue(vglPollingJobQueueManager.getQueue().hasJob());
        Assert.assertTrue((Boolean)mav.getModel().get("success"));
        Assert.assertEquals(JobBuilderController.STATUS_INQUEUE, jobObj.getStatus());
    }


    /**
     * Tests that job submission fails correctly when user specifies a storage service that DNE
     */
    @Test
    public void testJobSubmission_StorageServiceDNE() throws Exception {
        //Instantiate our job object
        final VEGLJob jobObj = new VEGLJob(new Integer(13));
        final String computeServiceId = "ccsid";
        final String injectedComputeVmId = "injected-compute-vmi-id";
        final String jobInSavedState = JobBuilderController.STATUS_UNSUBMITTED;
        final VglMachineImage[] mockImages = new VglMachineImage[] {context.mock(VglMachineImage.class)};
        final HashMap<String, Object> sessionVariables = new HashMap<String, Object>();
        final String storageServiceId = "cssid";

        sessionVariables.put("user-roles", new String[] {"testRole1", "testRole2"});
        jobObj.setComputeVmId(injectedComputeVmId);
        jobObj.setStatus(jobInSavedState); // by default, the job is in SAVED state
        jobObj.setComputeServiceId(computeServiceId);
        jobObj.setStorageServiceId("some-invalid-id");


        context.checking(new Expectations() {{
            //We should have 1 call to our job manager to get our job object and 1 call to save it
            oneOf(mockJobManager).getJobById(jobObj.getId());will(returnValue(jobObj));
            oneOf(mockJobManager).saveJob(jobObj);

            allowing(mockCloudComputeServices[0]).getId();will(returnValue(computeServiceId));
            allowing(mockCloudStorageServices[0]).getId();will(returnValue(storageServiceId));

            //We should have access control check to ensure user has permission to run the job
            oneOf(mockRequest).getSession();will(returnValue(mockSession));
            oneOf(mockSession).getAttribute("user-roles");will(returnValue(sessionVariables.get("user-roles")));
            oneOf(mockCloudComputeServices[0]).getAvailableImages();will(returnValue(mockImages));
            oneOf(mockImages[0]).getImageId();will(returnValue("compute-vmi-id"));
        }});

        ModelAndView mav = controller.submitJob(mockRequest, mockResponse, jobObj.getId().toString());

        Assert.assertFalse((Boolean)mav.getModel().get("success"));
        Assert.assertEquals(JobBuilderController.STATUS_UNSUBMITTED, jobObj.getStatus());
    }

    /**
     * Tests that job submission fails correctly when user specifies a compute service that DNE
     */
    @Test
    public void testJobSubmission_ComputeServiceDNE() throws Exception {
        //Instantiate our job object
        final VEGLJob jobObj = new VEGLJob(new Integer(13));
        final String computeServiceId = "ccsid";
        final String injectedComputeVmId = "injected-compute-vmi-id";
        final String jobInSavedState = JobBuilderController.STATUS_UNSUBMITTED;
        final VglMachineImage[] mockImages = new VglMachineImage[] {context.mock(VglMachineImage.class)};
        final HashMap<String, Object> sessionVariables = new HashMap<String, Object>();
        final String storageServiceId = "cssid";

        sessionVariables.put("user-roles", new String[] {"testRole1", "testRole2"});
        jobObj.setComputeVmId(injectedComputeVmId);
        jobObj.setStatus(jobInSavedState); // by default, the job is in SAVED state
        jobObj.setComputeServiceId("some-invalid-id");
        jobObj.setStorageServiceId(storageServiceId);


        context.checking(new Expectations() {{
            //We should have 1 call to our job manager to get our job object and 1 call to save it
            oneOf(mockJobManager).getJobById(jobObj.getId());will(returnValue(jobObj));
            oneOf(mockJobManager).saveJob(jobObj);

            allowing(mockCloudComputeServices[0]).getId();will(returnValue(computeServiceId));
            allowing(mockCloudStorageServices[0]).getId();will(returnValue(storageServiceId));

            //We should have access control check to ensure user has permission to run the job
            oneOf(mockRequest).getSession();will(returnValue(mockSession));
            oneOf(mockSession).getAttribute("user-roles");will(returnValue(sessionVariables.get("user-roles")));
            oneOf(mockCloudComputeServices[0]).getAvailableImages();will(returnValue(mockImages));
            oneOf(mockImages[0]).getImageId();will(returnValue("compute-vmi-id"));
        }});

        ModelAndView mav = controller.submitJob(mockRequest, mockResponse, jobObj.getId().toString());

        Assert.assertFalse((Boolean)mav.getModel().get("success"));
        Assert.assertEquals(JobBuilderController.STATUS_UNSUBMITTED, jobObj.getStatus());
    }

    /**
     * Tests that the bootstrap resource is not too long and has unix line endings and other such
     * conditions.
     * @throws Exception
     */
    @Test
    public void testBootstrapResource() throws Exception {
        //see - http://docs.amazonwebservices.com/AutoScaling/latest/APIReference/API_CreateLaunchConfiguration.html
        final int maxFileSize = 21847;
        final int safeFileSize = maxFileSize - 1024; //arbitrary number to account for long strings being injected into bootstrap

        String contents = ResourceUtil.loadResourceAsString("org/auscope/portal/server/web/controllers/vl-bootstrap.sh");

        Assert.assertNotNull(contents);
        Assert.assertTrue("Bootstrap is empty!", contents.length() > 0);
        Assert.assertTrue("Bootstrap is too big!", contents.length() < safeFileSize);
        Assert.assertFalse("Boostrap needs Unix style line endings!", contents.contains("\r"));
        Assert.assertEquals("Boostrap must start with '#'", '#', contents.charAt(0));

        //We can't use variables in the form ${name} as the {} conflict with java MessageFormat
        Pattern pattern = Pattern.compile("\\{(.*?)\\}");
        Matcher matcher = pattern.matcher(contents);
        while (matcher.find()) {

            if (matcher.groupCount() != 1) {
                continue;
            }
            String name = matcher.group(1);

            try {
                Integer.parseInt(name);
            } catch (NumberFormatException ex) {
                Assert.fail(String.format("The variable ${%1$s} conflicts with java MessageFormat variables. Get rid of curly braces", name));
            }
        }
    }

    /**
     * Tests that Grid Submit Controller's usage of the bootstrap template
     * @throws Exception
     */
    @Test
    public void testCreateBootstrapForJob() throws Exception {
        final VEGLJob job = new VEGLJob(1234);
        final String bucket = "stora124e-Bucket";
        final String access = "213-asd-54";
        final String secret = "tops3cret";
        final String provider = "provider";
        final String storageAuthVersion = "1.2.3";
        final String computeServiceId = "ccs";
        final String storageServiceId = "css";
        final String endpoint = "http://example.org";
        final String vmSh = "http://example2.org";
        final String regionName = "region-name";

        job.setComputeServiceId(computeServiceId);
        job.setStorageServiceId(storageServiceId);

        context.checking(new Expectations() {{
            //We allow calls to the Configurer which simply extract values from our property file
            allowing(mockHostConfigurer).resolvePlaceholder(with(equal("vm.sh")));will(returnValue(vmSh));
            atLeast(1).of(mockCloudStorageServices[0]).getId();will(returnValue(storageServiceId));
            atLeast(1).of(mockCloudStorageServices[0]).getBucket();will(returnValue(bucket));
            atLeast(1).of(mockCloudStorageServices[0]).getAccessKey();will(returnValue(access));
            atLeast(1).of(mockCloudStorageServices[0]).getSecretKey();will(returnValue(secret));
            atLeast(1).of(mockCloudStorageServices[0]).getProvider();will(returnValue(provider));
            atLeast(1).of(mockCloudStorageServices[0]).getAuthVersion();will(returnValue(storageAuthVersion));
            atLeast(1).of(mockCloudStorageServices[0]).getEndpoint();will(returnValue(endpoint));
            atLeast(1).of(mockCloudStorageServices[0]).getAuthVersion();will(returnValue(storageAuthVersion));
            atLeast(1).of(mockCloudStorageServices[0]).getRegionName();will(returnValue(regionName));
        }});

        job.setStorageBaseKey("test/key");

        String contents = controller.createBootstrapForJob(job);
        Assert.assertNotNull(contents);
        Assert.assertTrue("Bootstrap is empty!", contents.length() > 0);
        Assert.assertFalse("Boostrap needs Unix style line endings!", contents.contains("\r"));
        Assert.assertTrue(contents.contains(bucket));
        Assert.assertTrue(contents.contains(access));
        Assert.assertTrue(contents.contains(job.getStorageBaseKey()));
        Assert.assertTrue(contents.contains(secret));
        Assert.assertTrue(contents.contains(provider));
        Assert.assertTrue(contents.contains(endpoint));
        Assert.assertTrue(contents.contains(storageAuthVersion));
        Assert.assertTrue(contents.contains(vmSh));
        Assert.assertTrue(contents.contains(endpoint));
        Assert.assertTrue(contents.contains(regionName));
    }

    /**
     * Tests that Grid Submit Controller's usage of the bootstrap template correctly encodes an empty
     * string (when required)
     * @throws Exception
     */
    @Test
    public void testCreateBootstrapForJob_NoOptionalValues() throws Exception {
        final VEGLJob job = new VEGLJob(1234);
        final String bucket = "stora124e-Bucket";
        final String access = "213-asd-54";
        final String secret = "tops3cret";
        final String provider = "provider";
        final String storageAuthVersion = null;
        final String regionName = null;
        final String computeServiceId = "ccs";
        final String storageServiceId = "css";
        final String endpoint = "http://example.org";
        final String vmSh = "http://example2.org";

        job.setComputeServiceId(computeServiceId);
        job.setStorageServiceId(storageServiceId);

        context.checking(new Expectations() {{
            //We allow calls to the Configurer which simply extract values from our property file
            allowing(mockHostConfigurer).resolvePlaceholder(with(equal("vm.sh")));will(returnValue(vmSh));
            allowing(mockCloudStorageServices[0]).getId();will(returnValue(storageServiceId));
            allowing(mockCloudStorageServices[0]).getBucket();will(returnValue(bucket));
            allowing(mockCloudStorageServices[0]).getAccessKey();will(returnValue(access));
            allowing(mockCloudStorageServices[0]).getSecretKey();will(returnValue(secret));
            allowing(mockCloudStorageServices[0]).getProvider();will(returnValue(provider));
            allowing(mockCloudStorageServices[0]).getAuthVersion();will(returnValue(storageAuthVersion));
            allowing(mockCloudStorageServices[0]).getEndpoint();will(returnValue(endpoint));
            allowing(mockCloudStorageServices[0]).getAuthVersion();will(returnValue(storageAuthVersion));
            allowing(mockCloudStorageServices[0]).getRegionName();will(returnValue(regionName));
        }});

        job.setStorageBaseKey("test/key");

        String contents = controller.createBootstrapForJob(job);
        Assert.assertNotNull(contents);
        Assert.assertTrue(contents.contains("STORAGE_AUTH_VERSION=\"\""));
        Assert.assertTrue(contents.contains("OS_REGION_NAME=\"\""));
    }

    /**
     * Tests that listing job images for a user works as expected
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testListImages() throws Exception {
        final String computeServiceId = "compute-service-id";
        final VglMachineImage[] images = new VglMachineImage[] {context.mock(VglMachineImage.class)};

        context.checking(new Expectations() {{
            allowing(mockCloudComputeServices[0]).getId();will(returnValue(computeServiceId));
            oneOf(mockCloudComputeServices[0]).getAvailableImages();will(returnValue(images));

            oneOf(images[0]).getPermissions();will(returnValue(new String[] {"testRole2"}));
            allowing(mockRequest).isUserInRole("testRole2");will(returnValue(true));
        }});

        ModelAndView mav = controller.getImagesForComputeService(mockRequest, computeServiceId);
        Assert.assertNotNull(mav);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));
        Assert.assertNotNull(mav.getModel().get("data"));
        Assert.assertEquals(images.length, ((List) mav.getModel().get("data")).size());
    }

    /**
     * Tests that listing job images for a user works as expected when there is an image with no restrictions
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testListImages_NoRestrictions() throws Exception {
        final HashMap<String, Object> sessionVariables = new HashMap<String, Object>();
        final String computeServiceId = "compute-service-id";
        final VglMachineImage[] images = new VglMachineImage[] {context.mock(VglMachineImage.class)};

        sessionVariables.put("user-roles", new String[] {"testRole1", "testRole2"});

        context.checking(new Expectations() {{
            oneOf(mockRequest).getSession();will(returnValue(mockSession));
            oneOf(mockSession).getAttribute("user-roles");will(returnValue(sessionVariables.get("user-roles")));

            allowing(mockCloudComputeServices[0]).getId();will(returnValue(computeServiceId));
            oneOf(mockCloudComputeServices[0]).getAvailableImages();will(returnValue(images));

            oneOf(images[0]).getPermissions();will(returnValue(null));
        }});

        ModelAndView mav = controller.getImagesForComputeService(mockRequest, computeServiceId);
        Assert.assertNotNull(mav);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));
        Assert.assertNotNull(mav.getModel().get("data"));
        Assert.assertEquals(images.length, ((List) mav.getModel().get("data")).size());
    }

    /**
     * Tests the creation of a new job object.
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateOrCreateJob_CreateJobObject() throws Exception {
        final HashMap<String, Object> sessionVariables = new HashMap<String, Object>();
        final String storageProvider = "swift";
        final String storageEndpoint = "http://example.org/storage";
        final String baseKey = "base/key";
        final String storageServiceId = "storage-service";
        final String computeServiceId = "compute-service";
        final String computeVmType = "compute-vm-type";
        final String computeVmId = "compute-vm";
        final String name = "name";
        final String description = "desc";
        final Integer seriesId = 5431;
        final boolean emailNotification = true;

        sessionVariables.put("doubleValue", 123.45);
        sessionVariables.put("intValue", 123);
        sessionVariables.put("notExtracted", new Object()); //this should NOT be requested

        context.checking(new Expectations() {{
            //A whole bunch of parameters will be setup based on what session variables are set
            oneOf(mockRequest).getSession();will(returnValue(mockSession));

            oneOf(mockSession).getAttributeNames();will(returnValue(new IteratorEnumeration(sessionVariables.keySet().iterator())));
            allowing(mockSession).getAttribute("doubleValue");will(returnValue(sessionVariables.get("doubleValue")));
            allowing(mockSession).getAttribute("intValue");will(returnValue(sessionVariables.get("intValue")));;
            allowing(mockSession).getAttribute("notExtracted");will(returnValue(sessionVariables.get("notExtracted")));
            allowing(mockSession).getAttribute(JobDownloadController.SESSION_DOWNLOAD_LIST);will(returnValue(null));
            allowing(mockSession).setAttribute(JobDownloadController.SESSION_DOWNLOAD_LIST, null);

            allowing(mockPortalUser).getEmail();will(returnValue("email@example.org"));

            allowing(mockCloudComputeServices[0]).getId();will(returnValue(computeServiceId));

            oneOf(mockCloudStorageServices[0]).generateBaseKey(with(any(VEGLJob.class)));will(returnValue(baseKey));
            allowing(mockCloudStorageServices[0]).getId();will(returnValue(storageServiceId));

            oneOf(mockHostConfigurer).resolvePlaceholder("storage.provider");will(returnValue(storageProvider));
            oneOf(mockHostConfigurer).resolvePlaceholder("storage.endpoint");will(returnValue(storageEndpoint));

            oneOf(mockFileStagingService).generateStageInDirectory(with(any(VEGLJob.class)));

            oneOf(mockJobManager).saveJob(with(any(VEGLJob.class))); //one save job to get ID
            oneOf(mockJobManager).saveJob(with(any(VEGLJob.class))); //one save to finalise initialisation
            oneOf(mockJobManager).saveJob(with(any(VEGLJob.class))); //one save to include updates

            //We should have 1 call to our job manager to create a job audit trail record
            oneOf(mockJobManager).createJobAuditTrail(with(any(String.class)), with(any(VEGLJob.class)), with(any(String.class)));
        }});

        ModelAndView mav = controller.updateOrCreateJob(null,  //The integer ID if not specified will trigger job creation
                name,
                description,
                seriesId,
                computeServiceId,
                computeVmId,
                computeVmType,
                storageServiceId,
                null,
                emailNotification,
                mockRequest,
                mockPortalUser);

        Assert.assertNotNull(mav);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));

        List<VEGLJob> data = (List<VEGLJob>)mav.getModel().get("data");
        Assert.assertNotNull(data);
        Assert.assertEquals(1, data.size());

        VEGLJob newJob = data.get(0);
        Assert.assertNotNull(newJob);
        Assert.assertEquals(storageServiceId, newJob.getStorageServiceId());
        Assert.assertEquals(computeServiceId, newJob.getComputeServiceId());
        Assert.assertEquals(baseKey, newJob.getStorageBaseKey());
        Assert.assertEquals(computeVmType, newJob.getComputeInstanceType());

        Map<String, VglParameter> params = newJob.getJobParameters();
        Assert.assertNotNull(params);
        Assert.assertEquals(2, params.size());

        String paramToTest = "doubleValue";
        VglParameter param = params.get(paramToTest);
        Assert.assertNotNull(param);
        Assert.assertEquals("number", param.getType());
        Assert.assertEquals(sessionVariables.get(paramToTest).toString(), param.getValue());

        paramToTest = "intValue";
        param = params.get(paramToTest);
        Assert.assertNotNull(param);
        Assert.assertEquals("number", param.getType());
        Assert.assertEquals(sessionVariables.get(paramToTest).toString(), param.getValue());
    }

    /**
     * Tests that the updateJob works as expected
     * @throws Exception
     */
    @Test
    public void testUpdateOrCreateJob_UpdateJob() throws Exception {
        final VEGLJob mockJob = context.mock(VEGLJob.class);
        final int seriesId = 12;
        final int jobId = 1234;
        final String computeVmType = "compute-vm-type";
        final String newBaseKey = "base/key";
        final boolean emailNotification = true;

        context.checking(new Expectations() {{
            //We should have 1 call to our job manager to get our job object and 1 call to save it
            oneOf(mockJobManager).getJobById(jobId);will(returnValue(mockJob));

            //We should have the following fields updated
            oneOf(mockJob).setSeriesId(seriesId);
            oneOf(mockJob).setName("name");
            oneOf(mockJob).setDescription("description");
            oneOf(mockJob).setComputeVmId("computeVmId");
            oneOf(mockJob).setComputeServiceId("computeServiceId");
            oneOf(mockJob).setStorageServiceId("storageServiceId");
            oneOf(mockJob).setStorageBaseKey(newBaseKey);
            oneOf(mockJob).setEmailNotification(emailNotification);
            oneOf(mockJob).setComputeInstanceType(computeVmType);

            allowing(mockCloudComputeServices[0]).getId();will(returnValue("computeServiceId"));
            allowing(mockCloudStorageServices[0]).getId();will(returnValue("storageServiceId"));

            oneOf(mockCloudStorageServices[0]).generateBaseKey(mockJob);will(returnValue(newBaseKey));
            //We should have 1 call to save our job
            oneOf(mockJobManager).saveJob(mockJob);
        }});

        ModelAndView mav = controller.updateOrCreateJob(jobId,
                "name",
                "description",
                seriesId,
                "computeServiceId",
                "computeVmId",
                computeVmType,
                "storageServiceId",
                "registeredUrl",
                emailNotification,
                mockRequest,
                mockPortalUser);
        Assert.assertNotNull(mav);
        Assert.assertTrue((Boolean) mav.getModel().get("success"));
    }

    @Test
    public void testUpdateOrCreateJob_SaveFailure() throws Exception {
        final VEGLJob mockJob = context.mock(VEGLJob.class);
        final int seriesId = 12;
        final int jobId = 1234;
        final boolean emailNotification = true;
        final String computeVmType = "compute-vm-type";

        context.checking(new Expectations() {{
            //We should have 1 call to our job manager to get our job object and 1 call to save it
            oneOf(mockJobManager).getJobById(jobId);will(returnValue(mockJob));

            //We should have the following fields updated
            oneOf(mockJob).setSeriesId(seriesId);
            oneOf(mockJob).setName("name");
            oneOf(mockJob).setDescription("description");
            oneOf(mockJob).setComputeVmId("computeVmId");
            oneOf(mockJob).setComputeServiceId("computeServiceId");
            oneOf(mockJob).setStorageServiceId("storageServiceId");
            oneOf(mockJob).setEmailNotification(emailNotification);
            oneOf(mockJob).setComputeInstanceType(computeVmType);

            allowing(mockCloudComputeServices[0]).getId();will(returnValue("computeServiceId"));
            allowing(mockCloudStorageServices[0]).getId();will(returnValue("computeStorageId"));

            //We should have 1 call to save our job but will throw Exception
            oneOf(mockJobManager).saveJob(mockJob);will(throwException(new Exception("")));
        }});

        ModelAndView mav = controller.updateOrCreateJob(jobId,
                "name",
                "description",
                seriesId,
                "computeServiceId",
                "computeVmId",
                computeVmType,
                "storageServiceId",
                "registeredUrl",
                emailNotification,
                mockRequest,
                mockPortalUser);
        Assert.assertNotNull(mav);
        Assert.assertFalse((Boolean) mav.getModel().get("success"));
    }

    /**
     * Tests that the updateJob fails as expected with a bad storage id
     * @throws Exception
     */
    @Test
    public void testUpdateOrCreateJob_UpdateJobWithBadStorageId() throws Exception {
        final VEGLJob mockJob = context.mock(VEGLJob.class);
        final int seriesId = 12;
        final int jobId = 1234;
        final boolean emailNotification = true;
        final String computeVmType = "compute-vm-type";

        context.checking(new Expectations() {{
            //We should have 1 call to our job manager to get our job object and 1 call to save it
            oneOf(mockJobManager).getJobById(jobId);will(returnValue(mockJob));

            //We should have the following fields updated
            oneOf(mockJob).setSeriesId(seriesId);
            oneOf(mockJob).setName("name");
            oneOf(mockJob).setDescription("description");
            oneOf(mockJob).setComputeVmId("computeVmId");
            oneOf(mockJob).setComputeServiceId("computeServiceId");
            oneOf(mockJob).setStorageServiceId("storageServiceId");
            oneOf(mockJob).setEmailNotification(emailNotification);
            oneOf(mockJob).setComputeInstanceType(computeVmType);

            allowing(mockCloudComputeServices[0]).getId();will(returnValue("computeServiceId"));
            allowing(mockCloudStorageServices[0]).getId();will(returnValue("computeStorageId-thatDNE"));

            //We should have 1 call to save our job
            oneOf(mockJobManager).saveJob(mockJob);
        }});

        ModelAndView mav = controller.updateOrCreateJob(jobId,
                "name",
                "description",
                seriesId,
                "computeServiceId",
                "computeVmId",
                computeVmType,
                "storageServiceId",
                "registeredUrl",
                emailNotification,
                mockRequest,
                mockPortalUser);
        Assert.assertNotNull(mav);
        Assert.assertFalse((Boolean) mav.getModel().get("success"));
    }

    /**
     * Tests that the updateJob fails as expected with a bad compute id
     * @throws Exception
     */
    @Test
    public void testUpdateOrCreateJob_UpdateJobWithBadComputeId() throws Exception {
        final VEGLJob mockJob = context.mock(VEGLJob.class);
        final int seriesId = 12;
        final int jobId = 1234;
        final boolean emailNotification = true;
        final String computeVmType = "compute-vm-type";

        context.checking(new Expectations() {{
            //We should have 1 call to our job manager to get our job object and 1 call to save it
            oneOf(mockJobManager).getJobById(jobId);will(returnValue(mockJob));

            //We should have the following fields updated
            oneOf(mockJob).setSeriesId(seriesId);
            oneOf(mockJob).setName("name");
            oneOf(mockJob).setDescription("description");
            oneOf(mockJob).setComputeVmId("computeVmId");
            oneOf(mockJob).setComputeServiceId("computeServiceId");
            oneOf(mockJob).setStorageServiceId("storageServiceId");
            oneOf(mockJob).setEmailNotification(emailNotification);
            oneOf(mockJob).setComputeInstanceType(computeVmType);

            allowing(mockCloudComputeServices[0]).getId();will(returnValue("computeServiceId-thatDNE"));
            allowing(mockCloudStorageServices[0]).getId();will(returnValue("computeStorageId"));

            //We should have 1 call to save our job
            oneOf(mockJobManager).saveJob(mockJob);
        }});

        ModelAndView mav = controller.updateOrCreateJob(jobId,
                "name",
                "description",
                seriesId,
                "computeServiceId",
                "computeVmId",
                computeVmType,
                "storageServiceId",
                "registeredUrl",
                emailNotification,
                mockRequest,
                mockPortalUser);
        Assert.assertNotNull(mav);
        Assert.assertFalse((Boolean) mav.getModel().get("success"));
    }

    /**
     * Tests that the updateJobDownloads works as expected when appending
     * @throws Exception
     */
    @Test
    public void testUpdateJobDownloads_Append() throws Exception {
        final Integer jobId = 124;
        final String append = "true";
        final String[] names = new String[] {"n1", "n2"};
        final String[] descriptions = new String[] {"d1", "d2"};
        final String[] urls = new String[] {"http://example.org/1", "http://example.org/2"};
        final String[] localPaths = new String[] {"p1", "p2"};

        final VEGLJob myJob = new VEGLJob(jobId);
        final VglDownload[] existingDownloads = new VglDownload[] {new VglDownload(12356)};


        myJob.setJobDownloads(new ArrayList<VglDownload>(Arrays.asList(existingDownloads)));

        context.checking(new Expectations() {{
            oneOf(mockJobManager).getJobById(jobId);will(returnValue(myJob));

            oneOf(mockJobManager).saveJob(myJob);
        }});

        ModelAndView mav = controller.updateJobDownloads(jobId, append, names, descriptions, urls, localPaths);
        Assert.assertNotNull(mav);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));

        //The resulting job should have 3 elements in its list (due to appending)
        List<VglDownload> dls = myJob.getJobDownloads();
        Assert.assertEquals(existingDownloads.length + names.length, dls.size());
        Assert.assertSame(existingDownloads[0], dls.get(0));
        for (int i = 0; i < names.length; i++) {
            VglDownload dlToTest = dls.get(existingDownloads.length + i);
            Assert.assertEquals(names[i], dlToTest.getName());
            Assert.assertEquals(descriptions[i], dlToTest.getDescription());
            Assert.assertEquals(urls[i], dlToTest.getUrl());
            Assert.assertEquals(localPaths[i], dlToTest.getLocalPath());
        }
    }

    /**
     * Tests that the updateJobDownloads works as expected when replacing
     * @throws Exception
     */
    @Test
    public void testUpdateJobDownloads_Replace() throws Exception {
        final Integer jobId = 124;
        final String append = "false";
        final String[] names = new String[] {"n1", "n2"};
        final String[] descriptions = new String[] {"d1", "d2"};
        final String[] urls = new String[] {"http://example.org/1", "http://example.org/2"};
        final String[] localPaths = new String[] {"p1", "p2"};

        final VEGLJob myJob = new VEGLJob(jobId);
        final VglDownload[] existingDownloads = new VglDownload[] {new VglDownload(12356)};


        myJob.setJobDownloads(new ArrayList<VglDownload>(Arrays.asList(existingDownloads)));

        context.checking(new Expectations() {{
            oneOf(mockJobManager).getJobById(jobId);will(returnValue(myJob));

            oneOf(mockJobManager).saveJob(myJob);
        }});

        ModelAndView mav = controller.updateJobDownloads(jobId, append, names, descriptions, urls, localPaths);
        Assert.assertNotNull(mav);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));

        //The resulting job should have 3 elements in its list (due to appending)
        List<VglDownload> dls = myJob.getJobDownloads();
        Assert.assertEquals(names.length, dls.size());
        for (int i = 0; i < names.length; i++) {
            VglDownload dlToTest = dls.get(i);
            Assert.assertEquals(names[i], dlToTest.getName());
            Assert.assertEquals(descriptions[i], dlToTest.getDescription());
            Assert.assertEquals(urls[i], dlToTest.getUrl());
            Assert.assertEquals(localPaths[i], dlToTest.getLocalPath());
        }
    }

    /**
     * Unit test for succesfull object conversion in getAllJobInputs
     * @throws Exception
     */
    @Test
    public void testGetAllJobInputs() throws Exception {
        final Integer jobId = 543231;
        final VEGLJob job = new VEGLJob(jobId);
        final VglDownload dl = new VglDownload(413);
        final File mockFile = context.mock(File.class);
        final long mockFileLength = 21314L;
        final StagedFile[] stagedFiles = new StagedFile[]{new StagedFile(job, "another/file.ext", mockFile)};

        dl.setDescription("desc");
        dl.setEastBoundLongitude(1.0);
        dl.setWestBoundLongitude(2.0);
        dl.setLocalPath("local/path/file.ext");
        dl.setName("myFile");
        dl.setNorthBoundLatitude(3.0);
        dl.setSouthBoundLatitude(4.0);
        dl.setParent(job);

        job.setJobDownloads(Arrays.asList(dl));

        context.checking(new Expectations() {{
            oneOf(mockJobManager).getJobById(jobId);will(returnValue(job));
            oneOf(mockFile).length();will(returnValue(mockFileLength));
            oneOf(mockFileStagingService).listStageInDirectoryFiles(job);will(returnValue(stagedFiles));
        }});

        ModelAndView mav = controller.getAllJobInputs(jobId);
        Assert.assertNotNull(mav);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));

        Assert.assertNotNull(mav.getModel().get("data"));
        @SuppressWarnings("unchecked")
        List<VglDownload> fileInfo = (List<VglDownload>) mav.getModel().get("data");

        Assert.assertEquals(2, fileInfo.size());

        Assert.assertEquals(stagedFiles[0].getName(), fileInfo.get(0).getLocalPath());
        Assert.assertEquals(dl.getLocalPath(), fileInfo.get(1).getLocalPath());
    }

    /**
     * Simple test to test formatting of cloud service into ModelMap objects
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetComputeServices() throws Exception {
        final String name = "name";
        final String id = "id";

        context.checking(new Expectations() {{
            allowing(mockCloudComputeServices[0]).getName();will(returnValue(name));
            allowing(mockCloudComputeServices[0]).getId();will(returnValue(id));
        }});

        ModelAndView mav = controller.getComputeServices();

        Assert.assertNotNull(mav);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));

        ModelMap test = ((List<ModelMap>)mav.getModel().get("data")).get(0);

        Assert.assertEquals(name, test.get("name"));
        Assert.assertEquals(id, test.get("id"));
    }

    /**
     * Simple test to test formatting of cloud service into ModelMap objects
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetStorageServices() throws Exception {
        final String name = "name";
        final String id = "id";

        context.checking(new Expectations() {{
            allowing(mockCloudStorageServices[0]).getName();will(returnValue(name));
            allowing(mockCloudStorageServices[0]).getId();will(returnValue(id));
        }});

        ModelAndView mav = controller.getStorageServices();

        Assert.assertNotNull(mav);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));

        ModelMap test = ((List<ModelMap>)mav.getModel().get("data")).get(0);

        Assert.assertEquals(name, test.get("name"));
        Assert.assertEquals(id, test.get("id"));
    }

    /**
     * Tests that getting a compute type list for a particular compute service returns no exceptions
     * @throws Exception
     */
    @Test
    public void testGetComputeTypes() throws Exception {
        final String computeId = "compute-id";
        final ComputeType[] result = new ComputeType[] {new ComputeType("test-compute-type")};

        context.checking(new Expectations() {{
            allowing(mockCloudComputeServices[0]).getId();will(returnValue(computeId));
            allowing(mockCloudComputeServices[0]).getAvailableComputeTypes();will(returnValue(result));
        }});

        ModelAndView mav = controller.getTypesForComputeService(mockRequest, computeId);

        Assert.assertNotNull(mav);
        Assert.assertTrue((Boolean)mav.getModel().get("success"));
        Assert.assertSame(result, mav.getModel().get("data"));
    }

    /**
     * Tests that getting a compute type list for a particular compute service returns no exceptions
     * @throws Exception
     */
    @Test
    public void testGetComputeTypes_NoComputeService() throws Exception {
        final String computeId = "compute-id";

        context.checking(new Expectations() {{
            allowing(mockCloudComputeServices[0]).getId();will(returnValue(computeId));
        }});

        ModelAndView mav = controller.getTypesForComputeService(mockRequest, "non-matching-compute-id");

        Assert.assertNotNull(mav);
        Assert.assertFalse((Boolean)mav.getModel().get("success"));
    }

}