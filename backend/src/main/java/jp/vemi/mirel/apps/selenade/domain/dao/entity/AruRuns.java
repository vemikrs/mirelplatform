/*
 * Copyright(c) 2019 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.selenade.domain.dao.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * MSTE ステンシル
 */
@Entity
@Table(name = "aru_runs")
@Setter
@Getter
public class AruRuns {

    /** ID */
    @Id
    @Column
    public String id;

    /** 名前 */
    @Column
    public String name;

    /** 説明 */
    @Column
    public String explanation;

    /** 実行開始日時 */
    @Column
    public Date executeStartTime;

    /** 実行終了日時 */
    @Column
    public Date executeEndTime;
}
