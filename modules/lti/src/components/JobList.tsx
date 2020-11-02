import { Loading } from "./Loading";
import React from "react";
import { withTranslation, WithTranslation } from "react-i18next";
import {
    getJobs,
    JobResult,
} from "../OpencastRest";

interface JobListProps extends WithTranslation {
    readonly seriesId: string;
}

interface JobListState {
    readonly jobs?: JobResult[];
    readonly jobsTimerId?: ReturnType<typeof setTimeout>;
}

class TranslatedJobList extends React.Component<JobListProps, JobListState> {
    constructor(props: JobListProps) {
        super(props);
        this.state = {};
    }

    retrieveJobs() {
        getJobs(this.props.seriesId)
            .then((jobs) => this.setState({
                ...this.state,
                jobs: jobs
            })).catch((_) => this.setState({
                ...this.state,
                jobs: undefined
            }));
    }

    jobsTimer() {
        this.retrieveJobs();
    }

    componentDidMount() {
        this.retrieveJobs();

        const timerMillis = 1000;
        /* eslint react/no-did-mount-set-state: "off" */
        this.setState({
            ...this.state,
            jobsTimerId: setInterval(this.jobsTimer.bind(this), timerMillis),
        });
    }

    componentWillUnmount() {
        if (this.state.jobsTimerId !== undefined)
            clearInterval(this.state.jobsTimerId);
    }

    render() {
        if (this.state.jobs === undefined)
            return <Loading t={this.props.t} />;
        return <table className="table table-striped">
            <thead>
                <tr>
                    <th>{this.props.t("LTI.JOB_TITLE")}</th>
                    <th>{this.props.t("LTI.JOB_STATUS")}</th>
                </tr>
            </thead>
            <tbody>
                {this.state.jobs.map((job) => <tr key={job.title}>
                    <td>{job.title}</td>
                    <td>{this.props.t(job.status)}</td>
                </tr>)}
            </tbody>
        </table>;
    }
}


export const JobList = withTranslation()(TranslatedJobList);
